package calico.components.piemenu.grid;

import calico.Calico;
import calico.components.grid.CGrid;
import calico.components.piemenu.PieMenu;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CCanvasController;
import calico.inputhandlers.InputEventInfo;
import calico.networking.Networking;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;

public class CutCanvasButton extends PieMenuButton {

	private long uuid = 0L;
	public CutCanvasButton()
	{
		super("group.move");		
	}
	
	public void onClick(InputEventInfo ev)
	{
		//confirm with the user?
		//get the canvas ID
		long canvasClicked = CCanvasController.getCanvasAtPoint( PieMenu.lastOpenedPosition );		
		if(canvasClicked!=0l){
			CGrid.canvasAction=CGrid.CUT_CANVAS;
			CGrid.getInstance().drawSelectedCell(canvasClicked, ev.getX(), ev.getY());			
		}		
	}
}