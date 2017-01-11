class ServiceCache
  @@services = {}

  def self.get_service alias_name, service
    @@services[alias_name] ||= Spring.bean(service)
  end
end

class ServiceCacheStrategy
  def self.instance
    @@instance ||= Kernel.const_get(Rails.configuration.java_services_cache)
  end
end

module Services
  def self.service_with_alias_name(alias_name, bean_name)
    define_method alias_name do
      ServiceCacheStrategy.instance.get_service alias_name, bean_name
    end
  end

  def self.services(*args)
    args.each do |name|
      name = name.to_s
      service_with_alias_name(name, name.camelize(:lower))
    end
  end

  services(
    :admin_service,
    :agent_service,
    :artifacts_dir_holder,
    :artifacts_service,
    :backup_service,
    :cc_tray_service,
    :cc_tray_status_service,
    :changeset_service,
    :command_repository_service,
    :config_repository,
    :default_plugin_info_builder,
    :default_plugin_manager,
    :dependency_material_service,
    :elastic_profile_service,
    :entity_hashing_service,
    :environment_config_service,
    :environment_service,
    :environment_service,
    :failure_service,
    :feature_toggle_service,
    :flash_message_service,
    :garage_service,
    :go_cache,
    :go_config_dao,
    :go_config_service,
    :go_dashboard_service,
    :job_instance_service,
    :job_presentation_service,
    :localizer,
    :luau_service,
    :material_config_service,
    :material_service,
    :material_update_service,
    :mingle_config_service,
    :package_definition_service,
    :package_repository_service,
    :password_deserializer,
    :pipeline_config_service,
    :pipeline_configs_service,
    :pipeline_history_service,
    :pipeline_lock_service,
    :pipeline_pause_service,
    :pipeline_scheduler,
    :pipeline_selections_service,
    :pipeline_service,
    :pipeline_sql_map_dao,
    :pipeline_stages_feed_service,
    :pipeline_unlock_api_service,
    :pluggable_scm_service,
    :pluggable_task_service,
    :plugin_service,
    :properties_service,
    :role_config_service,
    :schedule_service,
    :security_auth_config_service,
    :security_service,
    :server_config_service,
    :server_health_service,
    :server_status_service,
    :shine_dao,
    :stage_service,
    :system_environment,
    :system_service,
    :task_view_service,
    :template_config_service,
    :user_search_service,
    :user_service,
    :value_stream_map_service,
    :version_info_service,
    :xml_api_service)

  service_with_alias_name(:go_config_service_for_url, "goConfigService")

  extend Services
end
