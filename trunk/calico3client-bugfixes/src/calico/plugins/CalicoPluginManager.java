package calico.plugins;

import it.unimi.dsi.fastutil.objects.*;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.log4j.Logger;

import calico.CalicoOptions;
import calico.components.piemenu.PieMenuButton;
import calico.plugins.events.*;
import calico.plugins.events.ui.*;

public class CalicoPluginManager
{
	
	public static Logger logger = Logger.getLogger(CalicoPluginManager.class.getName());
	
	// this maps classnames to actual plugin objects
	private static Object2ReferenceArrayMap<Class<?>, CalicoPlugin> plugins = new Object2ReferenceArrayMap<Class<?>, CalicoPlugin>();
	
	// this just lists all the objects
	private static ReferenceArrayList<CalicoPlugin> pluginList = new ReferenceArrayList<CalicoPlugin>();
	
	// This is where we store the event registrations
	private static Object2ObjectOpenHashMap<String, Class<?>> eventRegistry = new Object2ObjectOpenHashMap<String, Class<?>>(); 
	
	
	
	
	
	
	
	public static void RegisterPluginEvent(String eventName, Class<?> plugin)
	{
		
	}
	
	
	public static void ProcessPluginEvent(Class<?> plugin, CalicoEvent event)
	{
		
	}
	
	
	
	/**
	 * This will route the events to the plugins
	 * @param event
	 */
	public static void FireEvent(CalicoEvent event)
	{
		
		if(event.getClass().getSimpleName().equals("CalicoEvent"))
		{
		}
		else if(pluginList.size()>0)
		{
			String newMethodName = "on"+event.getClass().getSimpleName();
			for(int i=0;i<pluginList.size();i++)
			{
				try
				{
					CalicoPlugin plugin = pluginList.get(i);
					
					Method method = plugin.getClass().getMethod(newMethodName, event.getClass());
					try
					{
						method.invoke(plugin, event);
					}
					catch(Exception e)
					{
						plugin.onException(e);
					}
				}
				catch(NoSuchMethodException e){}
				catch(Exception e2)
				{
					e2.printStackTrace();
				}
			}	
		}
	}
	
	
	
	

	private static void registerPlugin(Class<?> pluginClass)
	{
		if(plugins.containsKey(pluginClass))
		{
			logger.error("Plugin "+pluginClass.getCanonicalName()+" has already been registered");
			return;
		}
		
		try
		{
			CalicoPlugin classObj = (CalicoPlugin) pluginClass.newInstance();
			
			try
			{
				logger.debug("Registering plugin "+classObj.PluginInfo.name);
				if (!classObj.registerNetworkCommandEvents())
				{
					System.err.println("PLUGIN WARNING: There is a command conflict with the plugin " + pluginClass.getName());
				}
				classObj.onPluginStart();
			}
			catch(Exception e)
			{
				classObj.onException(e);
			}
			
			plugins.put(pluginClass, classObj);
			pluginList.add(classObj);

		}
		catch (InstantiationException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
	}
	
	
	public static void shutdownPlugins()
	{
		logger.debug("Shutting down plugins");

		for(int i=0;i<pluginList.size();i++)
		{
			try
			{
				
				CalicoPlugin plugin = pluginList.get(i);
				logger.debug("Shutting down plugin "+plugin.PluginInfo.name);
				try
				{
					plugin.onPluginEnd();
				}
				catch(Exception e)
				{
					plugin.onException(e);
				}
			}
			catch(Exception e2)
			{
				e2.printStackTrace();
			}
		}
		logger.debug("All plugins shutdown");
		
	}
	
	
	
	
	
	public static void registerEvent(String eventName, Class<?> eventClass)
	{	
		eventRegistry.put(eventName, eventClass);
	}
	public static void registerEvent(Class<?> eventClass)
	{
		registerEvent(eventClass.getSimpleName(), eventClass);
	}
	public static Class<?> getEventClass(String eventName)
	{
		if(eventRegistry.containsKey(eventName))
		{
			return eventRegistry.get(eventName);
		}
		else
		{
			return null;
		}
	}
	
	
	
	
	
	public static void setup()
	{
		plugins.clear();
		pluginList.clear();
		pluginList.trim();
		eventRegistry.clear();
		// Register the default events
		registerEvent(calico.plugins.events.ui.ViewSingleCanvas.class);
		registerEvent(calico.plugins.events.ui.ViewGrid.class);
		
		/*registerEvent(calico.plugins.events.clients.ClientConnect.class);
		registerEvent(calico.plugins.events.clients.ClientDisconnect.class);

		registerEvent(calico.plugins.events.scraps.ScrapCreate.class);
		registerEvent(calico.plugins.events.scraps.ScrapDelete.class);
		registerEvent(calico.plugins.events.scraps.ScrapReload.class);
		*/
		
		//System.out.println("LOAD PLUGINS: "+COptions.server.plugins);
		
		logger.debug("Loading plugins");
		
		try
		{
			PluginFinder pluginFinder = new PluginFinder();
			pluginFinder.search("plugins/");
			List<Class<?>> pluginCollection = pluginFinder.getPluginCollection();
			for (Class<?> plugin: pluginCollection)
			{
				System.out.println("Loading " + plugin.getName());
				registerPlugin(plugin);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		try
		{
			registerPlugin(Class.forName("calico.plugins.palette.PalettePlugin"));
			registerPlugin(Class.forName("calico.plugins.userlist.UserListPlugin"));
			registerPlugin(Class.forName("calico.plugins.iip.IntentionalInterfacesClientPlugin"));
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		String[] pluginsToLoad = CalicoOptions.core.plugins.split(",");
		
		if(pluginsToLoad.length>0)
		{
			logger.debug("FIRST PLUGIN: "+pluginsToLoad[0]);
			for(int i=0;i<pluginsToLoad.length;i++)
			{
				if(pluginsToLoad[i].length()>0)
				{
					try
					{
						Class<?> pluginClass = Class.forName(pluginsToLoad[i].trim());
						registerPlugin(pluginClass);
					}
					catch(Exception e)
					{
						logger.error("Could not load plugin "+pluginsToLoad[i].trim());
					}
				}
			}
		}

//		registerPlugin(calico.plugins.palette.PalettePlugin.class);
	}
	
}
