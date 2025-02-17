// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: xai_service.proto

package gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated;

/**
 * Protobuf type {@code InitializationRequest}
 */
public  final class InitializationRequest extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:InitializationRequest)
    InitializationRequestOrBuilder {
  // Use InitializationRequest.newBuilder() to construct.
  private InitializationRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private InitializationRequest() {
    modelName_ = "";
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return com.google.protobuf.UnknownFieldSet.getDefaultInstance();
  }
  private InitializationRequest(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    int mutable_bitField0_ = 0;
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          default: {
            if (!input.skipField(tag)) {
              done = true;
            }
            break;
          }
          case 10: {
            java.lang.String s = input.readStringRequireUtf8();

            modelName_ = s;
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.GrpcProto.internal_static_InitializationRequest_descriptor;
  }

  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.GrpcProto.internal_static_InitializationRequest_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest.class, gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest.Builder.class);
  }

  public static final int MODEL_NAME_FIELD_NUMBER = 1;
  private volatile java.lang.Object modelName_;
  /**
   * <code>string model_name = 1;</code>
   */
  public java.lang.String getModelName() {
    java.lang.Object ref = modelName_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      modelName_ = s;
      return s;
    }
  }
  /**
   * <code>string model_name = 1;</code>
   */
  public com.google.protobuf.ByteString
      getModelNameBytes() {
    java.lang.Object ref = modelName_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      modelName_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  private byte memoizedIsInitialized = -1;
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (!getModelNameBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, modelName_);
    }
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (!getModelNameBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, modelName_);
    }
    memoizedSize = size;
    return size;
  }

  private static final long serialVersionUID = 0L;
  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest)) {
      return super.equals(obj);
    }
    gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest other = (gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest) obj;

    boolean result = true;
    result = result && getModelName()
        .equals(other.getModelName());
    return result;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (37 * hash) + MODEL_NAME_FIELD_NUMBER;
    hash = (53 * hash) + getModelName().hashCode();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code InitializationRequest}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:InitializationRequest)
      gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequestOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.GrpcProto.internal_static_InitializationRequest_descriptor;
    }

    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.GrpcProto.internal_static_InitializationRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest.class, gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest.Builder.class);
    }

    // Construct using gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
      }
    }
    public Builder clear() {
      super.clear();
      modelName_ = "";

      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.GrpcProto.internal_static_InitializationRequest_descriptor;
    }

    public gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest getDefaultInstanceForType() {
      return gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest.getDefaultInstance();
    }

    public gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest build() {
      gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest buildPartial() {
      gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest result = new gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest(this);
      result.modelName_ = modelName_;
      onBuilt();
      return result;
    }

    public Builder clone() {
      return (Builder) super.clone();
    }
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        Object value) {
      return (Builder) super.setField(field, value);
    }
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return (Builder) super.clearField(field);
    }
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return (Builder) super.clearOneof(oneof);
    }
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, Object value) {
      return (Builder) super.setRepeatedField(field, index, value);
    }
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        Object value) {
      return (Builder) super.addRepeatedField(field, value);
    }
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest) {
        return mergeFrom((gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest other) {
      if (other == gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest.getDefaultInstance()) return this;
      if (!other.getModelName().isEmpty()) {
        modelName_ = other.modelName_;
        onChanged();
      }
      onChanged();
      return this;
    }

    public final boolean isInitialized() {
      return true;
    }

    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private java.lang.Object modelName_ = "";
    /**
     * <code>string model_name = 1;</code>
     */
    public java.lang.String getModelName() {
      java.lang.Object ref = modelName_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        modelName_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string model_name = 1;</code>
     */
    public com.google.protobuf.ByteString
        getModelNameBytes() {
      java.lang.Object ref = modelName_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        modelName_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string model_name = 1;</code>
     */
    public Builder setModelName(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      modelName_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string model_name = 1;</code>
     */
    public Builder clearModelName() {
      
      modelName_ = getDefaultInstance().getModelName();
      onChanged();
      return this;
    }
    /**
     * <code>string model_name = 1;</code>
     */
    public Builder setModelNameBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      modelName_ = value;
      onChanged();
      return this;
    }
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return this;
    }

    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return this;
    }


    // @@protoc_insertion_point(builder_scope:InitializationRequest)
  }

  // @@protoc_insertion_point(class_scope:InitializationRequest)
  private static final gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest();
  }

  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<InitializationRequest>
      PARSER = new com.google.protobuf.AbstractParser<InitializationRequest>() {
    public InitializationRequest parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
        return new InitializationRequest(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<InitializationRequest> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<InitializationRequest> getParserForType() {
    return PARSER;
  }

  public gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.InitializationRequest getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

