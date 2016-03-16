class TestServiceCache
  extend RSpec::Mocks::ExampleMethods

  @services = {}
  @config = {}

  def self.get_service alias_name, service_bean_name
    @services[alias_name] ||= (@config[:use_stubs] ? double(alias_name) : Spring.bean(service_bean_name))
  end

  def self.clear_services
    @services = {}
  end

  def self.replace_service alias_name, service_to_replace_with
    @services[alias_name] = service_to_replace_with
  end

  def self.[]= key, value
    @config[key] = value
  end

  def self.[] key
    @config[key]
  end
end