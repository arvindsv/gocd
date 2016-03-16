module MiscSpecExtensions
  def java_date_utc(year, month, day, hour, minute, second)
    org.joda.time.DateTime.new(year, month, day, hour, minute, second, 0, org.joda.time.DateTimeZone::UTC).toDate()
  end

  def stub_server_health_messages
    assign(:current_server_health_states, com.thoughtworks.go.serverhealth.ServerHealthStates.new)
  end

  def stub_server_health_messages_for_controllers
    assigns[:current_server_health_states] = com.thoughtworks.go.serverhealth.ServerHealthStates.new
  end

  def current_user
    @user ||= com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new("some-user"), "display name")
    @controller.stub(:current_user).and_return(@user)
    @user
  end

  def setup_base_urls
    config_service = Spring.bean("goConfigService")
    if (config_service.currentCruiseConfig().server().getSiteUrl().getUrl().nil?)
      config_service.updateConfig(Class.new do
        def update config
          server = config.server()
          com.thoughtworks.go.util.ReflectionUtil.setField(server, "siteUrl", com.thoughtworks.go.domain.ServerSiteUrlConfig.new("http://test.host"))
          com.thoughtworks.go.util.ReflectionUtil.setField(server, "secureSiteUrl", com.thoughtworks.go.domain.ServerSiteUrlConfig.new("https://ssl.host:443"))
          return config
        end
      end.new)
    end
  end

  def cdata_wraped_regexp_for(value)
    /<!\[CDATA\[#{value}\]\]>/
  end

  def fake_template_presence file_path, content
    controller.prepend_view_path(ActionView::FixtureResolver.new(file_path => content))
  end

  def stub_service(service_getter)
    ServiceCacheStrategy.instance.replace_service service_getter.to_s, double(service_getter.to_s) unless ServiceCacheStrategy.instance[:use_stubs]
    controller.send(service_getter)
  end

  def stub_localized_result
    result = com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult.new
    com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult.stub(:new).and_return(result)
    result
  end

  def stub_localizer
    allow_any_instance_of(RailsLocalizer::L).to receive(:method_missing) do |method, *args|
      "LOC_#{method}_#{args[0]}"
    end
    # allow_any_instance_of(RailsLocalizer::L).to receive(:string) do |key|
    #   "DONT_CARE_ABOUT_#{key}"
    # end
    controller.stub(:l).and_return(RailsLocalizer::L.new)
  end

  def setup_localized_msg_for options = {}
    expected_params = [options[:key]]
    expected_params = [eq(options[:key]), match_array(options[:params].to_java(java.lang.String))] if options.key?(:params)
    STDERR.puts "Expecting: #{expected_params}"
    allow_any_instance_of(RailsLocalizer::L).to receive(:string).with(*expected_params).and_return(options[:value])
  end

  def ignore_flash_message_service
    allow(controller.flash_message_service).to receive(:add)
  end

  def stub_url_for_in_application_controller
    controller.stub(:url_for) do |options_arg|
      ActionController::Base.instance_method(:url_for).bind(controller).call(options_arg)
    end
  end
end