package lame.laven.msgs;

public enum SkeletonMsg {
   user_dir,
   entering_phase_pom,
   entering_phase_dir,

   pom_should_create,
   pom_creating,
   pom_prompt_pom_value,
   pom_model,
   pom_groupid,
   pom_groupid_def,
   pom_artifactid,
   pom_artifactid_def,
   pom_version,
   pom_version_def,
   pom_project_artifactid,

   pom_prop_java_version,
   pom_prop_java_version_def,
   pom_prop_encoding_source,
   pom_prop_encoding_report,
   pom_prop_utf8,
   pom_prop_including,

   pom_prop_skip_adding,
   pom_prop_adding,
   pom_prop_prompt_name,
   pom_prop_prompt_value,

   pom_dep_skip_adding,
   pom_dep_adding,
   pom_dep_add_dependency,
   pom_dep_add_scope,
   pom_dep_dependency_regex,
   ex_pom_dep_format,

   pom_depmgt_skip,
   pom_depmgt_groupid,
   pom_depmgt_artifactid,
   pom_depmgt_version,
   pom_depmgt_type,
   pom_depmgt_scope,

   pom_build_comp_artifactid,
   pom_build_comp_version,
   pom_build_comp_config,
   pom_build_comp_config_source,
   pom_build_comp_config_target,
   pom_build_comp_config_java_ver_ref,

   pom_should_write,
   pom_filename,
   pom_writting_file,
   pom_overwrite_file,
   ex_pom_delete,
   ex_pom_write,

   pom_review,
   ex_pom_review,

   dir_should_create,
   dir_creating,
   dir_exists,
   ex_dir_create,
   ex_dir_path,

   prompt_format,
   prompt_yes_no,

}
