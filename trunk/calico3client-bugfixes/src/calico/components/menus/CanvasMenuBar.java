package calico.components.menus;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;

import calico.*;
import calico.components.*;
import calico.components.grid.*;
import calico.components.menus.buttons.*;
import calico.controllers.CCanvasController;
import calico.iconsets.CalicoIconManager;
import calico.input.CInputMode;
import calico.inputhandlers.*;
import calico.modules.*;
import calico.networking.*;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;

import edu.umd.cs.piccolo.*;
import edu.umd.cs.piccolo.util.*;
import edu.umd.cs.piccolo.nodes.*;
import edu.umd.cs.piccolox.nodes.*;
import edu.umd.cs.piccolox.pswing.*;

import java.net.*;

import javax.swing.JOptionPane;

import edu.umd.cs.piccolo.event.*;



public class CanvasMenuBar extends CanvasGenericMenuBar
{
	private static final long serialVersionUID = 1L;
	
	private long cuid = 0L;
	
	private PImage lockButton;
	int setLock_button_array_index;
	Rectangle setLock_bounds;
	
	private PImage clients;
	
	private static ObjectArrayList<Class<?>> externalButtons = new ObjectArrayList<Class<?>>();
	private static ObjectArrayList<Class<?>> externalButtonsPreAppended = new ObjectArrayList<Class<?>>();
	private static ObjectArrayList<Class<?>> externalButtons_rightAligned = new ObjectArrayList<Class<?>>();
	
	public CanvasMenuBar(long c, int screenPos)
	{		
		super(screenPos, CCanvasController.canvasdb.get(c).getBounds());		
		
		cuid = c;
		
		Rectangle rect_default = new Rectangle(0,0,20,20);
		
		addCap(CanvasGenericMenuBar.ALIGN_START);
		
		try
		{
			for (Class<?> button : externalButtonsPreAppended)
			{
				if (button.getClass().getName().compareTo(SpacerButton.class.getName()) == 0)
					addSpacer();
				else
					addIcon((CanvasMenuButton) button.getConstructor(long.class).newInstance(cuid));
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
//		clients = addText(
//				CCanvasController.canvasdb.get(cuid).getClients().length+" clients", 
//				new Font("Verdana", Font.BOLD, 12),
//				new CanvasTextButton(cuid) {
//					public void actionMouseClicked(Rectangle boundingBox) {
//						CCanvasController.canvasdb.get(cuid).drawClientList(boundingBox);
//					}
//				}
//				);
		
//		addSpacer();
		
		
		// viewport buttons
//		addIcon(new ViewportChangeButton(cuid, ViewportChangeButton.BUT_ZOOMIN));
//		addIcon(new ViewportChangeButton(cuid, ViewportChangeButton.BUT_ZOOMOUT));
//		addIcon(new ViewportChangeButton(cuid, ViewportChangeButton.BUT_ZOOMTOCANVAS));
		
		
//		addSpacer();
		

		//addCap();
//		addIcon(new DoNotEraseButton(cuid));
//		addIcon(new CanEraseButton(cuid));
		addIcon(new ClearButton(cuid));
		addSpacer();
		
//		setLock_button_array_index = text_button_array_index++;
//		setLock();
//		addSpacer();
		
		addIcon(new UndoButton(cuid));
		addIcon(new RedoButton(cuid));
		addSpacer();
		
		for(int i=0;i<CalicoOptions.menu.colorlist.length;i++)
		{
			addIcon(new MBColorButton(cuid, CalicoOptions.menu.colorlist[i], CalicoOptions.menu.colorlist_icons[i], rect_default));
		}
		addSpacer();
		
		for(int i=0;i<CalicoOptions.menu.pensize.length;i++)
		{
			addIcon(new MBSizeButton(cuid, CalicoOptions.menu.pensize[i], CalicoOptions.menu.pensize_icons[i], rect_default));
		}
//		addIcon(new MBModeChangeButton(cuid, CInputMode.ARROW));
		addSpacer();
		
		// Mode buttons
		addIcon(new MBModeChangeButton(cuid, CInputMode.DELETE));

//		addIcon(new MBModeChangeButton(cuid, CInputMode.EXPERT));
		addIcon(new MBModeChangeButton(cuid, CInputMode.POINTER));
		
		addSpacer();
		addIcon(new TextCreateButton(cuid));
		addIcon(new ImageCreateButton(cuid));
		
		
		
		
		//Begin align right

		
	//	addSpacer();addSpacer();
		//addIcon(new MBDeveloperButton(cuid, "canvas_clear"));
		
		try
		{
			for (Class<?> button : externalButtons)
			{
				addSpacer();
				addIcon((CanvasMenuButton) button.getConstructor(long.class).newInstance(cuid));
			}
			
			for (Class<?> button : externalButtons_rightAligned)
			{
				addSpacer(ALIGN_END);
				addIconRightAligned((CanvasMenuButton) button.getConstructor(long.class).newInstance(cuid));
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		
		//this.invalidatePaint();
		CalicoDraw.invalidatePaint(this);
	}
	
	private void setLock()
	{
		String text = (CCanvasController.canvasdb.get(cuid).getLockValue())?"DO NOT ERASE":"   CAN ERASE   ";
		
		
		
		setLock(text, new Font("Verdana", Font.BOLD, 12),
				new CanvasTextButton(cuid) {
			public void actionMouseClicked(InputEventInfo event, Rectangle boundingBox) {
				if (event.getAction() == InputEventInfo.ACTION_PRESSED)
				{
					isPressed = true;
				}
				else if (event.getAction() == InputEventInfo.ACTION_RELEASED && isPressed)
				{
					boolean lockValue = CCanvasController.canvasdb.get(cuid).getLockValue();
					long time = (new Date()).getTime();
					CCanvasController.lock_canvas(cuid, !lockValue, CalicoDataStore.Username, time);
	//				setLock();
					CCanvasController.canvasdb.get(cuid).drawMenuBars();
					
					isPressed = false;
				}
			}
		});
		
	}
	
	protected void setLock(String text, Font font, CanvasTextButton buttonHandler)
	{
		/*PText gct = new PText(text);
		gct.setConstrainWidthToTextWidth(true);
		gct.setConstrainHeightToTextHeight(true);
		gct.setFont(font);//new Font("Monospaced", Font.BOLD, 20));
		Rectangle rect_coordtxt = addIcon(gct.getBounds().getBounds());
		gct.setBounds(rect_coordtxt);
		addChild(0,gct);
		*/
		
		if (lockButton != null)
		{
			//removeChild(lockButton);
			CalicoDraw.removeChildFromNode(this, lockButton);
		}
		
		Image img = getTextImage(text,font);

		int imgSpan = 0;
		switch (this.position)
		{
			case POSITION_TOP:
			case POSITION_BOTTOM:
				imgSpan = img.getWidth(null);
				break;
			case POSITION_LEFT:
			case POSITION_RIGHT:
				imgSpan = img.getHeight(null);
				break;
		}		
		
		if (setLock_bounds == null)
			setLock_bounds = addIcon(imgSpan);
		
		lockButton = new PImage();
		
		lockButton.setImage(img);
		
		lockButton.setBounds(setLock_bounds);
		
		super.text_rect_array[setLock_button_array_index] = setLock_bounds;

		text_button_array[setLock_button_array_index] = buttonHandler;
		
		//addChild(0,lockButton);
		CalicoDraw.addChildToNode(this, lockButton, 0);
	}
	
	private void changeLock()
	{
		
	}
	
	
	
	
	public void redrawColorIcon()
	{
		
	}
	
	
	public void redrawArrowIndicator()
	{

	}

	public void redrawClients() {
		
		Rectangle bounds = clients.getBounds().getBounds();
		
		
		
		PImage newClients = new PImage();
		newClients.setImage(getTextImage(CCanvasController.canvasdb.get(cuid).getClients().length+" clients", 
				new Font("Verdana", Font.BOLD, 12)));
		newClients.setBounds(bounds);
		//addChild(0, newClients);
		CalicoDraw.addChildToNode(this, newClients, 0);
		//removeChild(clients);
		CalicoDraw.removeChildFromNode(this, clients);
		
		clients = newClients;
		
	}
	
	public static void addMenuButtonPreAppend(Class<?> button)
	{
		externalButtonsPreAppended.add(button);
	}
	
	public static void addMenuButton(Class<?> button)
	{
		externalButtons.add(button);
	}
	
	public static void addMenuButtonRightAligned(Class<?> button)
	{
		externalButtons_rightAligned.add(button);
	}
	
	public static void removeMenuButton(Class<?> button)
	{
		externalButtons.remove(button);
	}
	
		
}
