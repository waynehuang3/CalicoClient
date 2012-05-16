package calico.controllers;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import it.unimi.dsi.fastutil.longs.Long2ReferenceAVLTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import org.apache.log4j.Logger;

import calico.Geometry;
import calico.components.CConnector;
import calico.components.CStroke;
import calico.components.arrow.CArrow;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.piemenu.PieMenuButton;
import calico.networking.Networking;
import calico.networking.PacketHandler;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;

public class CConnectorController {
	/**
	 * Logger
	 */
	private static Logger logger = Logger.getLogger(CConnectorController.class.getName());
	
	/**
	 * This is the database of all the BGElements
	 */
	public static Long2ReferenceAVLTreeMap<CConnector> connectors = new Long2ReferenceAVLTreeMap<CConnector>();
	
	public static boolean exists(long uuid)
	{
		return connectors.containsKey(uuid);
	}
	
	private static long getConnectorCanvasUUID(long uuid)
	{
		return connectors.get(uuid).getCanvasUUID();
	}
	
	//For now, this only happens on the client. The server gets the connector via a CONNECTOR_LOAD command.
	public static void create(long uuid, long cuid, Color color, float thickness, Polygon points, long anchorHead, long anchorTail)
	{
		no_notify_create(uuid, cuid, color, thickness, points, anchorHead, anchorTail);
		
		Networking.send(connectors.get(uuid).getUpdatePackets()[0]);
	}
	
	/**
	 * This will create a Connector, but not notify anybody
	 * @param uuid
	 * @param cuid
	 * @param puid
	 * @param color
	 */
	public static void no_notify_create(long uuid, long cuid, Color color, float thickness, Polygon points, long anchorHead, long anchorTail)
	{
		// does the element already exist?
		if(exists(uuid))
		{
			no_notify_delete(uuid);
		}
		// add to the DB
		if (anchorHead == 0l && anchorTail == 0l)
		{
			connectors.put(uuid, new CConnector(uuid, cuid, color, thickness, points));
		}
		else
		{
			connectors.put(uuid, new CConnector(uuid, cuid, color, thickness, points, anchorHead, anchorTail));
		}
		
		// Add to the canvas
		CCanvasController.no_notify_add_child_connector(cuid, uuid);
		
		// We need to notify the groups 
		CGroupController.no_notify_add_connector(connectors.get(uuid).getAnchorUUID(CConnector.TYPE_HEAD), uuid);
		CGroupController.no_notify_add_connector(connectors.get(uuid).getAnchorUUID(CConnector.TYPE_TAIL), uuid);	
	}
	
	/**
	 * This will create a Connector, but not notify anybody
	 * @param uuid
	 * @param cuid
	 * @param puid
	 * @param color
	 */
	public static void no_notify_create(long uuid, long cuid, Color color, float thickness, Point head, Point tail, double[] orthogonalDistance, 
			double[] travelDistance, long anchorHead, long anchorTail)
	{
		// does the element already exist?
		if(exists(uuid))
		{
			no_notify_delete(uuid);
		}
		// add to the DB
		connectors.put(uuid, new CConnector(uuid, cuid, color, thickness, head, tail, orthogonalDistance, travelDistance, anchorHead, anchorTail));
		
		// Add to the canvas
		CCanvasController.no_notify_add_child_connector(cuid, uuid);
		
		// We need to notify the groups 
		CGroupController.no_notify_add_connector(connectors.get(uuid).getAnchorUUID(CConnector.TYPE_HEAD), uuid);
		CGroupController.no_notify_add_connector(connectors.get(uuid).getAnchorUUID(CConnector.TYPE_TAIL), uuid);	
	}
	
	public static void delete(long uuid)
	{
		no_notify_delete(uuid);
		
		Networking.send(NetworkCommand.CONNECTOR_DELETE, uuid);
	}
	
	public static void no_notify_delete(long uuid)
	{
		if (!exists(uuid))
			return;
			
		connectors.get(uuid).delete();
		connectors.remove(uuid);
	}
	
	public static void linearize(long uuid)
	{
		no_notify_linearize(uuid);
		
		Networking.send(NetworkCommand.CONNECTOR_LINEARIZE, uuid);
	}
	
	public static void no_notify_linearize(long uuid)
	{
		if (!exists(uuid))
			return;
		
		connectors.get(uuid).linearize();
		
		if (BubbleMenu.isBubbleMenuActive() && BubbleMenu.activeUUID == uuid)
		{
			BubbleMenu.moveIconPositions(connectors.get(uuid).getBounds());
		}
	}
	
	public static void no_notify_move_group_anchor(long uuid, long guuid, int x, int y)
	{
		if (!exists(uuid))
			return;
		
		connectors.get(uuid).moveAnchor(guuid, x, y);
	}
	
	public static void make_stroke(long uuid)
	{
		CalicoPacket[] p = connectors.get(uuid).getStrokePackets();
		delete(uuid);
		batchReceive(p);
		Networking.send(p[0]);
	}
	
	private static void batchReceive(CalicoPacket[] packets)
	{
		for (int i = 0; i < packets.length; i++)
		{
			if (packets[i] == null)
			{
				logger.warn("WARNING!!! BatchReceive received a null packet, something likely went wrong!");
				continue;
			}
			
			CalicoPacket p = new CalicoPacket(packets[i].getBuffer());
			PacketHandler.receive(p);
		}
	}
	
	public static boolean intersectsCircle(long suuid, Point center, double radius)
	{
		Polygon stroke = connectors.get(suuid).getPolygon();

		for (int i = 1; i < stroke.npoints; i++)
		{
			double circleDist = Line2D.ptSegDist(stroke.xpoints[i-1], stroke.ypoints[i-1], stroke.xpoints[i], stroke.ypoints[i], center.getX(), center.getY());
			if (circleDist < radius)
				return true;
		}
		return false;
	}
	
	public static long getNearestConnector(Point p, int maxDistance)
	{
		if (p == null)
			return 0l;
		
		long[] connectors = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getChildConnectors();
		long closestConnector = 0l;
		double minStrokeDistance = java.lang.Double.MAX_VALUE;
		Polygon temp;
		for (int i = 0; i < connectors.length; i++)
		{
			temp = CConnectorController.connectors.get(connectors[i]).getPolygon();
			double minSegmentDistance = java.lang.Double.MAX_VALUE;
			for (int j = 0; j < temp.npoints - 1; j++)
			{
				double[] intersectPoint = Geometry.computeIntersectingPoint(temp.xpoints[j], temp.ypoints[j], temp.xpoints[j+1], temp.ypoints[j+1], p.x, p.y);
				double AtoB = Geometry.length(temp.xpoints[j], temp.ypoints[j], temp.xpoints[j+1], temp.ypoints[j+1]);
				double AtoI = Geometry.length(temp.xpoints[j], temp.ypoints[j], intersectPoint[0], intersectPoint[1]);
				double ItoB = Geometry.length(intersectPoint[0], intersectPoint[1], temp.xpoints[j+1], temp.ypoints[j+1]);
				double actualDistance;
				
				//The intersecting point is not on the segment
				if (AtoI > AtoB || ItoB > AtoB)
				{
					actualDistance = Math.min(Geometry.length(temp.xpoints[j], temp.ypoints[j], p.x, p.y),
								Geometry.length(p.x, p.y, temp.xpoints[j+1], temp.ypoints[j+1]));
				}
				//The intersecting line is on the segment
				else
				{
					actualDistance = Geometry.length(intersectPoint[0], intersectPoint[1], p.x, p.y);
				}
				if (actualDistance < minSegmentDistance)
					minSegmentDistance = actualDistance;
			}
			
			if (minSegmentDistance < maxDistance && minSegmentDistance < minStrokeDistance)
			{
				minStrokeDistance = minSegmentDistance;
				closestConnector = connectors[i];
			}
		}
		return closestConnector;
	}
	
	public static void show_stroke_bubblemenu(long uuid, boolean fade)
	{
		//Class<?> pieMenuClass = calico.components.piemenu.PieMenu.class;
		if (!exists(uuid))
			return;

		ObjectArrayList<Class<?>> pieMenuButtons = CConnectorController.connectors.get(uuid).getBubbleMenuButtons();
		
		int[] bitmasks = new int[pieMenuButtons.size()];
		
		
		
		if(pieMenuButtons.size()>0)
		{
			ArrayList<PieMenuButton> buttons = new ArrayList<PieMenuButton>();
			
			for(int i=0;i<pieMenuButtons.size();i++)
			{
				try
				{
					buttons.add((PieMenuButton) pieMenuButtons.get(i).getConstructor(long.class).newInstance(uuid));
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}

			BubbleMenu.displayBubbleMenu(uuid,fade,BubbleMenu.TYPE_CONNECTOR,buttons.toArray(new PieMenuButton[buttons.size()]));
			
			
		}
		
		
	}
	
	public static int get_signature(long l) {
		if (!exists(l))
			return 0;
		
		return connectors.get(l).get_signature();
	}
}