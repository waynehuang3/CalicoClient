package calico.plugins.iip.components.piemenu.iip;

import calico.components.piemenu.PieMenuButton;
import calico.inputhandlers.InputEventInfo;
import calico.plugins.iip.components.CCanvasLink;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.iconsets.CalicoIconManager;
import calico.plugins.iip.inputhandlers.CIntentionCellInputHandler;

public class CreateLinkButton extends PieMenuButton
{
	public CreateLinkButton()
	{
		super(CalicoIconManager.getIconImage("intention.link-canvas"));
	}

	@Override
	public void onPressed(InputEventInfo event)
	{
		super.onPressed(event);
		
		CreateIntentionArrowPhase.INSTANCE.startCreate(
				CIntentionCellController.getInstance().getCellById(CIntentionCellInputHandler.getInstance().getActiveCell()), event.getGlobalPoint(),
				CreateIntentionArrowPhase.NewLinkMode.LINK_EXISTING);
	}
}
