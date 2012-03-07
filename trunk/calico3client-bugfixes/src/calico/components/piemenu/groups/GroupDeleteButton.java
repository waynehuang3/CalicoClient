package calico.components.piemenu.groups;

import calico.Calico;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.iconsets.CalicoIconManager;
import calico.inputhandlers.InputEventInfo;

public class GroupDeleteButton extends PieMenuButton
{
	public static int SHOWON = PieMenuButton.SHOWON_SCRAP_CREATE | PieMenuButton.SHOWON_SCRAP_MENU;
	private boolean isActive = false;
	long uuidToBeDeleted;
	
	public GroupDeleteButton(long uuid)
	{
		super("group.delete");
		uuidToBeDeleted = uuid;
	}
	
	public void onPressed(InputEventInfo ev)
	{
		if (!CGroupController.exists(uuidToBeDeleted) || isActive)
		{
			return;
		}
		
		isActive = true;
		super.onPressed(ev);
	}
	
	public void onReleased(InputEventInfo ev)
	{
		CGroupController.delete(uuidToBeDeleted);
		ev.stop();
		
		Calico.logger.debug("CLICKED GROUP DELETE BUTTON");
		isActive = false;
	}
}
