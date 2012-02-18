package calico.plugins.iip.components.piemenu.iip;

import calico.components.piemenu.PieMenuButton;
import calico.plugins.iip.components.CCanvasLink;
import calico.plugins.iip.iconsets.CalicoIconManager;

public class CreateNewIdeaLinkButton extends PieMenuButton
{
	public CreateNewIdeaLinkButton()
	{
		super(CCanvasLink.LinkType.NEW_IDEA.image);
	}
	
	@Override
	public void onClick()
	{
		System.out.println("Start creating a new idea arrow from here");
	}
}
