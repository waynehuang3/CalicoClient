package calico.inputhandlers.canvas;

import calico.*;

import calico.components.*;
import calico.components.menus.*;
import calico.components.piemenu.*;
import calico.controllers.*;
import calico.iconsets.CalicoIconManager;
import calico.inputhandlers.CCanvasInputHandler;
import calico.inputhandlers.CalicoAbstractInputHandler;
import calico.inputhandlers.CalicoInputManager;
import calico.inputhandlers.InputEventInfo;
import calico.networking.*;
import calico.networking.netstuff.*;


import java.awt.geom.*;
import java.awt.*;

import java.util.*;

import org.apache.log4j.*;

import edu.umd.cs.piccolo.event.*;
import edu.umd.cs.piccolo.nodes.PImage;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PBounds;


// implements PenListener
public class CCanvasEraseModeInputHandler extends CalicoAbstractInputHandler
{
	public static Logger logger = Logger.getLogger(CCanvasEraseModeInputHandler.class.getName());

	public static final double CREATE_GROUP_MIN_DIST = 15.0;
	

	private boolean hasStartedGroup = false;
	private boolean hasStartedBge = false;
	
	private boolean hasBeenPressed = false;
	private boolean hasBeenMoved = false;

	private InputEventInfo lastEvent = null;
	
//	private Polygon draggedCoords = new Polygon();

	private CCanvasInputHandler parentHandler = null;

	private static String iconName = "mode.delete";
	
	private PPath eraser;
	
	boolean erasedSomething = false;

	public CCanvasEraseModeInputHandler(long cuid, CCanvasInputHandler parent)
	{
		canvas_uid = cuid;
		parentHandler = parent;
		
//		this.setupModeIcon();
		
	}
	
	public void actionPressed(InputEventInfo e)
	{
//		draggedCoords = new Polygon();
//		draggedCoords.addPoint(e.getX(), e.getY());
		lastEvent = e;
		
		//draw eraser circle

		drawCircle(e);

		CalicoInputManager.drawCursorImage(CCanvasController.getCurrentUUID(), CalicoIconManager.getIconImage(iconName), e.getPoint());
		Networking.send(NetworkCommand.ERASE_START, canvas_uid);
		erasedSomething = false;
//		this.showModeIcon(e.getPoint());
	}



	public void actionDragged(InputEventInfo e)
	{
//		this.hideModeIcon(e.getPoint());
//		draggedCoords.addPoint(e.getX(), e.getY());

//		CalicoInputManager.lockInputHandler(canvas_uid);

		//int x = e.getX();
		//int y = e.getY();

		drawCircle(e);
		Ellipse2D.Double eraseRectangle = new Ellipse2D.Double(e.getPoint().x - CalicoOptions.pen.eraser.radius, e.getPoint().y - CalicoOptions.pen.eraser.radius, CalicoOptions.pen.eraser.radius*2, CalicoOptions.pen.eraser.radius*2);
		
		if( (e.isLeftButtonPressed() || e.isMiddleButtonPressed() ))
		{
			long[] bgelist = CCanvasController.canvasdb.get(canvas_uid).getChildStrokes();
			if(bgelist.length>0)
			{
				for(int i=0;i<bgelist.length;i++)
				{
					if(CStrokeController.exists(bgelist[i]) && CStrokeController.intersectsCircle(bgelist[i],e.getPoint(), CalicoOptions.pen.eraser.radius) )
					{
						CStrokeController.delete(bgelist[i]);
						erasedSomething = true;
						System.out.println("Deleting Stroke. Storke ID: " + bgelist[i] + ", parent: " + CStrokeController.strokes.get(bgelist[i]).getParentUUID() + ", i = " + i + ", line: (" + lastEvent.getPoint().getX() + "," + lastEvent.getPoint().getY() + ") (" + e.getPoint().getX() + "," + e.getPoint().getY());
					}
				}
			}
			
			long[] arrowlist = CCanvasController.canvasdb.get(canvas_uid).getChildArrows();
			if(arrowlist.length>0)
			{
				for(int i=0;i<arrowlist.length;i++)
				{
					if(CArrowController.exists(arrowlist[i]) && CArrowController.intersectsCircle(arrowlist[i],e.getPoint(), CalicoOptions.pen.eraser.radius) )
					{
						logger.debug("DELETE ARROW "+arrowlist[i]);
						CArrowController.delete(arrowlist[i]);
						erasedSomething = true;
					}
				}
			}
		}
		
		lastEvent = e;
	}
	
	public void actionScroll(InputEventInfo e)
	{
	}
	

	public void actionReleased(InputEventInfo e)
	{
//		this.hideModeIcon();
		CalicoInputManager.unlockHandlerIfMatch(canvas_uid);
		Networking.send(NetworkCommand.ERASE_START, canvas_uid, erasedSomething);
		
		removeCircle();
		
		lastEvent = e;
		erasedSomething = false;
	}
	
	private void drawCircle(InputEventInfo e) {
		if (eraser == null)
		{
		Ellipse2D.Double hitTarget = new Ellipse2D.Double(e.getPoint().x - CalicoOptions.pen.eraser.radius, e.getPoint().y - CalicoOptions.pen.eraser.radius, CalicoOptions.pen.eraser.radius*2, CalicoOptions.pen.eraser.radius*2);
		eraser = new PPath(hitTarget);
		eraser.setStrokePaint(Color.black);
		eraser.setStroke(new BasicStroke(1.0f));
		CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer().addChild(eraser);
		eraser.invalidatePaint();
		CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer().repaintFrom(eraser.getBounds(), eraser);
		}
		else {
			Rectangle origBounds = eraser.getBounds().getBounds();
			Ellipse2D.Double hitTarget = new Ellipse2D.Double(e.getPoint().x - CalicoOptions.pen.eraser.radius, e.getPoint().y - CalicoOptions.pen.eraser.radius, CalicoOptions.pen.eraser.radius*2, CalicoOptions.pen.eraser.radius*2);
			eraser.setPathTo(hitTarget);
			eraser.repaint();
			CCanvasController.canvasdb.get(canvas_uid).repaint(new PBounds(origBounds));
//			eraser.moveTo((float)(e.getPoint().x - CalicoOptions.pen.eraser.radius), (float)(e.getPoint().y - CalicoOptions.pen.eraser.radius));
		}
	}
	
	private void removeCircle()
	{
		PBounds bounds = eraser.getBounds();
		CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer().removeChild(eraser);
		CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer().repaintFrom(bounds, null);
		eraser = null;
	}
}
