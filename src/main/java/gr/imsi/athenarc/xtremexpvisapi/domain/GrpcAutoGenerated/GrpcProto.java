// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: xai_service.proto

package gr.imsi.athenarc.xtremexpvisapi.domain.GrpcAutoGenerated;

public final class GrpcProto {
  private GrpcProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_InitializationRequest_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_InitializationRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_ModelAnalysisTaskRequest_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_ModelAnalysisTaskRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_Feature_Explanation_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_Feature_Explanation_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_Feature_Explanation_PlotsEntry_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_Feature_Explanation_PlotsEntry_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_Feature_Explanation_TablesEntry_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_Feature_Explanation_TablesEntry_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_InitializationResponse_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_InitializationResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_ModelAnalysisTaskResponse_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_ModelAnalysisTaskResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_Hyperparameter_Explanation_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_Hyperparameter_Explanation_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_Hyperparameter_Explanation_PlotsEntry_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_Hyperparameter_Explanation_PlotsEntry_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_Hyperparameter_Explanation_TablesEntry_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_Hyperparameter_Explanation_TablesEntry_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_ExplanationsRequest_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_ExplanationsRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_Features_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_Features_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_Axis_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_Axis_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_TableContents_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_TableContents_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_ExplanationsResponse_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_ExplanationsResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_ExplanationsResponse_TableContentsEntry_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_ExplanationsResponse_TableContentsEntry_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\021xai_service.proto\"+\n\025InitializationReq" +
      "uest\022\022\n\nmodel_name\030\001 \001(\t\"@\n\030ModelAnalysi" +
      "sTaskRequest\022\022\n\nmodel_name\030\001 \001(\t\022\020\n\010mode" +
      "l_id\030\002 \001(\005\"\231\002\n\023Feature_Explanation\022\025\n\rfe" +
      "ature_names\030\001 \003(\t\022.\n\005plots\030\002 \003(\0132\037.Featu" +
      "re_Explanation.PlotsEntry\0220\n\006tables\030\003 \003(" +
      "\0132 .Feature_Explanation.TablesEntry\032C\n\nP" +
      "lotsEntry\022\013\n\003key\030\001 \001(\t\022$\n\005value\030\002 \001(\0132\025." +
      "ExplanationsResponse:\0028\001\032D\n\013TablesEntry\022" +
      "\013\n\003key\030\001 \001(\t\022$\n\005value\030\002 \001(\0132\025.Explanatio",
      "nsResponse:\0028\001\"\214\001\n\026InitializationRespons" +
      "e\0221\n\023feature_explanation\030\001 \001(\0132\024.Feature" +
      "_Explanation\022?\n\032hyperparameter_explanati" +
      "on\030\002 \001(\0132\033.Hyperparameter_Explanation\"N\n" +
      "\031ModelAnalysisTaskResponse\0221\n\023feature_ex" +
      "planation\030\001 \001(\0132\024.Feature_Explanation\"\265\002" +
      "\n\032Hyperparameter_Explanation\022\034\n\024hyperpar" +
      "ameter_names\030\001 \003(\t\0225\n\005plots\030\002 \003(\0132&.Hype" +
      "rparameter_Explanation.PlotsEntry\0227\n\006tab" +
      "les\030\003 \003(\0132\'.Hyperparameter_Explanation.T",
      "ablesEntry\032C\n\nPlotsEntry\022\013\n\003key\030\001 \001(\t\022$\n" +
      "\005value\030\002 \001(\0132\025.ExplanationsResponse:\0028\001\032" +
      "D\n\013TablesEntry\022\013\n\003key\030\001 \001(\t\022$\n\005value\030\002 \001" +
      "(\0132\025.ExplanationsResponse:\0028\001\"\361\001\n\023Explan" +
      "ationsRequest\022\030\n\020explanation_type\030\001 \001(\t\022" +
      "\032\n\022explanation_method\030\002 \001(\t\022\r\n\005model\030\003 \001" +
      "(\t\022\020\n\010model_id\030\004 \001(\005\022\020\n\010feature1\030\005 \001(\t\022\020" +
      "\n\010feature2\030\006 \001(\t\022\027\n\017num_influential\030\007 \001(" +
      "\005\022\025\n\rproxy_dataset\030\010 \001(\014\022\r\n\005query\030\t \001(\t\022" +
      "\020\n\010features\030\n \001(\t\022\016\n\006target\030\013 \001(\t\".\n\010Fea",
      "tures\022\020\n\010feature1\030\001 \001(\t\022\020\n\010feature2\030\002 \001(" +
      "\t\"A\n\004Axis\022\021\n\taxis_name\030\001 \001(\t\022\023\n\013axis_val" +
      "ues\030\002 \003(\t\022\021\n\taxis_type\030\003 \001(\t\".\n\rTableCon" +
      "tents\022\r\n\005index\030\001 \001(\005\022\016\n\006values\030\002 \003(\t\"\216\003\n" +
      "\024ExplanationsResponse\022\033\n\023explainability_" +
      "type\030\001 \001(\t\022\032\n\022explanation_method\030\002 \001(\t\022\034" +
      "\n\024explainability_model\030\003 \001(\t\022\021\n\tplot_nam" +
      "e\030\004 \001(\t\022\022\n\nplot_descr\030\005 \001(\t\022\021\n\tplot_type" +
      "\030\006 \001(\t\022\033\n\010features\030\007 \001(\0132\t.Features\022\024\n\005x" +
      "Axis\030\010 \001(\0132\005.Axis\022\024\n\005yAxis\030\t \001(\0132\005.Axis\022",
      "\024\n\005zAxis\030\n \001(\0132\005.Axis\022@\n\016table_contents\030" +
      "\013 \003(\0132(.ExplanationsResponse.TableConten" +
      "tsEntry\032D\n\022TableContentsEntry\022\013\n\003key\030\001 \001" +
      "(\t\022\035\n\005value\030\002 \001(\0132\016.TableContents:\0028\0012\334\001" +
      "\n\014Explanations\022=\n\016GetExplanation\022\024.Expla" +
      "nationsRequest\032\025.ExplanationsResponse\022A\n" +
      "\016Initialization\022\026.InitializationRequest\032" +
      "\027.InitializationResponse\022J\n\021ModelAnalysi" +
      "sTask\022\031.ModelAnalysisTaskRequest\032\032.Model" +
      "AnalysisTaskResponseBG\n8gr.imsi.athenarc",
      ".xtremexpvisapi.domain.GrpcAutoGenerated" +
      "B\tGrpcProtoP\001b\006proto3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
    internal_static_InitializationRequest_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_InitializationRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_InitializationRequest_descriptor,
        new java.lang.String[] { "ModelName", });
    internal_static_ModelAnalysisTaskRequest_descriptor =
      getDescriptor().getMessageTypes().get(1);
    internal_static_ModelAnalysisTaskRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_ModelAnalysisTaskRequest_descriptor,
        new java.lang.String[] { "ModelName", "ModelId", });
    internal_static_Feature_Explanation_descriptor =
      getDescriptor().getMessageTypes().get(2);
    internal_static_Feature_Explanation_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_Feature_Explanation_descriptor,
        new java.lang.String[] { "FeatureNames", "Plots", "Tables", });
    internal_static_Feature_Explanation_PlotsEntry_descriptor =
      internal_static_Feature_Explanation_descriptor.getNestedTypes().get(0);
    internal_static_Feature_Explanation_PlotsEntry_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_Feature_Explanation_PlotsEntry_descriptor,
        new java.lang.String[] { "Key", "Value", });
    internal_static_Feature_Explanation_TablesEntry_descriptor =
      internal_static_Feature_Explanation_descriptor.getNestedTypes().get(1);
    internal_static_Feature_Explanation_TablesEntry_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_Feature_Explanation_TablesEntry_descriptor,
        new java.lang.String[] { "Key", "Value", });
    internal_static_InitializationResponse_descriptor =
      getDescriptor().getMessageTypes().get(3);
    internal_static_InitializationResponse_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_InitializationResponse_descriptor,
        new java.lang.String[] { "FeatureExplanation", "HyperparameterExplanation", });
    internal_static_ModelAnalysisTaskResponse_descriptor =
      getDescriptor().getMessageTypes().get(4);
    internal_static_ModelAnalysisTaskResponse_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_ModelAnalysisTaskResponse_descriptor,
        new java.lang.String[] { "FeatureExplanation", });
    internal_static_Hyperparameter_Explanation_descriptor =
      getDescriptor().getMessageTypes().get(5);
    internal_static_Hyperparameter_Explanation_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_Hyperparameter_Explanation_descriptor,
        new java.lang.String[] { "HyperparameterNames", "Plots", "Tables", });
    internal_static_Hyperparameter_Explanation_PlotsEntry_descriptor =
      internal_static_Hyperparameter_Explanation_descriptor.getNestedTypes().get(0);
    internal_static_Hyperparameter_Explanation_PlotsEntry_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_Hyperparameter_Explanation_PlotsEntry_descriptor,
        new java.lang.String[] { "Key", "Value", });
    internal_static_Hyperparameter_Explanation_TablesEntry_descriptor =
      internal_static_Hyperparameter_Explanation_descriptor.getNestedTypes().get(1);
    internal_static_Hyperparameter_Explanation_TablesEntry_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_Hyperparameter_Explanation_TablesEntry_descriptor,
        new java.lang.String[] { "Key", "Value", });
    internal_static_ExplanationsRequest_descriptor =
      getDescriptor().getMessageTypes().get(6);
    internal_static_ExplanationsRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_ExplanationsRequest_descriptor,
        new java.lang.String[] { "ExplanationType", "ExplanationMethod", "Model", "ModelId", "Feature1", "Feature2", "NumInfluential", "ProxyDataset", "Query", "Features", "Target", });
    internal_static_Features_descriptor =
      getDescriptor().getMessageTypes().get(7);
    internal_static_Features_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_Features_descriptor,
        new java.lang.String[] { "Feature1", "Feature2", });
    internal_static_Axis_descriptor =
      getDescriptor().getMessageTypes().get(8);
    internal_static_Axis_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_Axis_descriptor,
        new java.lang.String[] { "AxisName", "AxisValues", "AxisType", });
    internal_static_TableContents_descriptor =
      getDescriptor().getMessageTypes().get(9);
    internal_static_TableContents_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_TableContents_descriptor,
        new java.lang.String[] { "Index", "Values", });
    internal_static_ExplanationsResponse_descriptor =
      getDescriptor().getMessageTypes().get(10);
    internal_static_ExplanationsResponse_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_ExplanationsResponse_descriptor,
        new java.lang.String[] { "ExplainabilityType", "ExplanationMethod", "ExplainabilityModel", "PlotName", "PlotDescr", "PlotType", "Features", "XAxis", "YAxis", "ZAxis", "TableContents", });
    internal_static_ExplanationsResponse_TableContentsEntry_descriptor =
      internal_static_ExplanationsResponse_descriptor.getNestedTypes().get(0);
    internal_static_ExplanationsResponse_TableContentsEntry_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_ExplanationsResponse_TableContentsEntry_descriptor,
        new java.lang.String[] { "Key", "Value", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
