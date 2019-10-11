package cs555.system.util;

/**
 * 
 * @author stock
 *
 */
public interface Properties {

  final String CONF_NAME = "application.properties";

  final String DISCOVERY_HOST =
      Configurations.getInstance().getProperty( "discovery.host" );

  final int DISCOVERY_PORT = Integer
      .parseInt( Configurations.getInstance().getProperty( "discovery.port" ) );

  final String SYSTEM_LOG_LEVEL =
      Configurations.getInstance().getProperty( "system.log.level", "INFO" );

  final String SYSTEM_DHT_STYLE =
      Configurations.getInstance().getProperty( "system.dht.style", "SHORT" );
  
}
