package gr.imsi.athenarc.xtremexpvisapi.service.files;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Maps opaque, server-issued file IDs to absolute local file paths.
 *
 * <p>Clients never see or supply filesystem paths: a file is registered server-side (e.g. right
 * after a data asset is downloaded/cached) and the resulting ID is handed back to the client. The
 * ID can later be resolved to serve the file's bytes. This avoids the path-traversal /
 * arbitrary-file-read risk of accepting a raw {@code path} request parameter, because only files
 * the server itself registered can ever be served.
 *
 * <p>The store is in-memory and intentionally ephemeral, mirroring the cache it backs (cached
 * assets are auto-deleted after the configured TTL). Stale entries are pruned lazily on resolve.
 */
@Service
public class FileRegistry {

  private final Map<String, String> idToPath = new ConcurrentHashMap<>();

  /**
   * Registers a local file path and returns a stable, opaque ID for it. The ID is derived
   * deterministically from the absolute path, so registering the same file repeatedly yields the
   * same ID (no duplicate entries).
   *
   * @param localPath the absolute or relative local path of the file
   * @return an opaque ID that can later be passed to {@link #resolve(String)}
   */
  public String register(String localPath) {
    String absolute = Paths.get(localPath).toAbsolutePath().normalize().toString();
    String id = hash(absolute);
    idToPath.put(id, absolute);
    return id;
  }

  /**
   * Resolves a previously registered ID back to its absolute file path.
   *
   * @param id the opaque ID returned by {@link #register(String)}
   * @return the absolute path, or {@code null} if the ID is unknown or the file no longer exists
   */
  public String resolve(String id) {
    String path = idToPath.get(id);
    if (path == null) {
      return null;
    }
    // Self-clean stale entries: cached files are deleted once their TTL elapses.
    if (!Files.exists(Paths.get(path))) {
      idToPath.remove(id);
      return null;
    }
    return path;
  }

  private static String hash(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(bytes.length * 2);
      for (byte b : bytes) {
        sb.append(Character.forDigit((b >> 4) & 0xF, 16));
        sb.append(Character.forDigit(b & 0xF, 16));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 is guaranteed to be present on every JVM.
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }
}
