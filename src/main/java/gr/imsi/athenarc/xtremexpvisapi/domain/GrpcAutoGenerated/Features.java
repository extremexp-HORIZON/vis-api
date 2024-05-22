// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: xai_service.proto

package gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated;

/**
 * Protobuf type {@code Features}
 */
public  final class Features extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:Features)
    FeaturesOrBuilder {
  // Use Features.newBuilder() to construct.
  private Features(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private Features() {
    feature1_ = "";
    feature2_ = "";
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return com.google.protobuf.UnknownFieldSet.getDefaultInstance();
  }
  private Features(
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

            feature1_ = s;
            break;
          }
          case 18: {
            java.lang.String s = input.readStringRequireUtf8();

            feature2_ = s;
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
    return gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.GrpcProto.internal_static_Features_descriptor;
  }

  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.GrpcProto.internal_static_Features_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features.class, gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features.Builder.class);
  }

  public static final int FEATURE1_FIELD_NUMBER = 1;
  private volatile java.lang.Object feature1_;
  /**
   * <code>string feature1 = 1;</code>
   */
  public java.lang.String getFeature1() {
    java.lang.Object ref = feature1_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      feature1_ = s;
      return s;
    }
  }
  /**
   * <code>string feature1 = 1;</code>
   */
  public com.google.protobuf.ByteString
      getFeature1Bytes() {
    java.lang.Object ref = feature1_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      feature1_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int FEATURE2_FIELD_NUMBER = 2;
  private volatile java.lang.Object feature2_;
  /**
   * <code>string feature2 = 2;</code>
   */
  public java.lang.String getFeature2() {
    java.lang.Object ref = feature2_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      feature2_ = s;
      return s;
    }
  }
  /**
   * <code>string feature2 = 2;</code>
   */
  public com.google.protobuf.ByteString
      getFeature2Bytes() {
    java.lang.Object ref = feature2_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      feature2_ = b;
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
    if (!getFeature1Bytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, feature1_);
    }
    if (!getFeature2Bytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, feature2_);
    }
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (!getFeature1Bytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, feature1_);
    }
    if (!getFeature2Bytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, feature2_);
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
    if (!(obj instanceof gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features)) {
      return super.equals(obj);
    }
    gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features other = (gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features) obj;

    boolean result = true;
    result = result && getFeature1()
        .equals(other.getFeature1());
    result = result && getFeature2()
        .equals(other.getFeature2());
    return result;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (37 * hash) + FEATURE1_FIELD_NUMBER;
    hash = (53 * hash) + getFeature1().hashCode();
    hash = (37 * hash) + FEATURE2_FIELD_NUMBER;
    hash = (53 * hash) + getFeature2().hashCode();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features parseFrom(
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
  public static Builder newBuilder(gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features prototype) {
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
   * Protobuf type {@code Features}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:Features)
      gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.FeaturesOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.GrpcProto.internal_static_Features_descriptor;
    }

    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.GrpcProto.internal_static_Features_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features.class, gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features.Builder.class);
    }

    // Construct using gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features.newBuilder()
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
      feature1_ = "";

      feature2_ = "";

      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.GrpcProto.internal_static_Features_descriptor;
    }

    public gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features getDefaultInstanceForType() {
      return gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features.getDefaultInstance();
    }

    public gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features build() {
      gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features buildPartial() {
      gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features result = new gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features(this);
      result.feature1_ = feature1_;
      result.feature2_ = feature2_;
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
      if (other instanceof gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features) {
        return mergeFrom((gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features other) {
      if (other == gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features.getDefaultInstance()) return this;
      if (!other.getFeature1().isEmpty()) {
        feature1_ = other.feature1_;
        onChanged();
      }
      if (!other.getFeature2().isEmpty()) {
        feature2_ = other.feature2_;
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
      gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private java.lang.Object feature1_ = "";
    /**
     * <code>string feature1 = 1;</code>
     */
    public java.lang.String getFeature1() {
      java.lang.Object ref = feature1_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        feature1_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string feature1 = 1;</code>
     */
    public com.google.protobuf.ByteString
        getFeature1Bytes() {
      java.lang.Object ref = feature1_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        feature1_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string feature1 = 1;</code>
     */
    public Builder setFeature1(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      feature1_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string feature1 = 1;</code>
     */
    public Builder clearFeature1() {
      
      feature1_ = getDefaultInstance().getFeature1();
      onChanged();
      return this;
    }
    /**
     * <code>string feature1 = 1;</code>
     */
    public Builder setFeature1Bytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      feature1_ = value;
      onChanged();
      return this;
    }

    private java.lang.Object feature2_ = "";
    /**
     * <code>string feature2 = 2;</code>
     */
    public java.lang.String getFeature2() {
      java.lang.Object ref = feature2_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        feature2_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string feature2 = 2;</code>
     */
    public com.google.protobuf.ByteString
        getFeature2Bytes() {
      java.lang.Object ref = feature2_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        feature2_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string feature2 = 2;</code>
     */
    public Builder setFeature2(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      feature2_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string feature2 = 2;</code>
     */
    public Builder clearFeature2() {
      
      feature2_ = getDefaultInstance().getFeature2();
      onChanged();
      return this;
    }
    /**
     * <code>string feature2 = 2;</code>
     */
    public Builder setFeature2Bytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      feature2_ = value;
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


    // @@protoc_insertion_point(builder_scope:Features)
  }

  // @@protoc_insertion_point(class_scope:Features)
  private static final gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features();
  }

  public static gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<Features>
      PARSER = new com.google.protobuf.AbstractParser<Features>() {
    public Features parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
        return new Features(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<Features> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<Features> getParserForType() {
    return PARSER;
  }

  public gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated.Features getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

