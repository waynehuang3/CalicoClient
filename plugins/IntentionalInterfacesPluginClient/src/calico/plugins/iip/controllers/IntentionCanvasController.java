package calico.plugins.iip.controllers;

import it.unimi.dsi.fastutil.longs.Long2ReferenceArrayMap;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import calico.CalicoDataStore;
import calico.controllers.CCanvasController;
import calico.plugins.iip.components.CCanvasLink;
import calico.plugins.iip.components.CCanvasLinkAnchor;
import calico.plugins.iip.components.canvas.CCanvasLinkBadge;
import calico.plugins.iip.components.canvas.CCanvasLinkToken;
import calico.plugins.iip.components.canvas.CanvasIntentionToolBar;
import calico.plugins.iip.components.canvas.CanvasLinkBay;

public class IntentionCanvasController
{
	public static IntentionCanvasController getInstance()
	{
		return INSTANCE;
	}

	public static void initialize()
	{
		INSTANCE = new IntentionCanvasController();
	}

	private static IntentionCanvasController INSTANCE;


	private Long2ReferenceArrayMap<CCanvasLinkBadge> badgesByAnchorId = new Long2ReferenceArrayMap<CCanvasLinkBadge>();
	private Long2ReferenceArrayMap<List<Long>> badgeAnchorIdsByCanvasId = new Long2ReferenceArrayMap<List<Long>>();

	private Long2ReferenceArrayMap<CCanvasLinkToken> tokensByAnchorId = new Long2ReferenceArrayMap<CCanvasLinkToken>();
	private Long2ReferenceArrayMap<List<Long>> tokenAnchorIdsByCanvasId = new Long2ReferenceArrayMap<List<Long>>();

	private long currentCanvasId = 0L;
	private final CanvasLinkBay incomingLinkBay = new CanvasLinkBay(currentCanvasId, CCanvasLink.LinkDirection.INCOMING, new UpperLeftLayout());
	private final CanvasLinkBay outgoingLinkBay = new CanvasLinkBay(currentCanvasId, CCanvasLink.LinkDirection.OUTGOING, new LowerRightLayout());

	private Comparator<CCanvasLinkToken> sorter = new DefaultSorter();

	public void addLink(CCanvasLink link)
	{
		switch (link.getLinkType())
		{
			case DESIGN_INSIDE:
				addBadges(link);
				break;
			default:
				addTokens(link);
				break;
		}
	}
	
	public void removeLink(CCanvasLink link)
	{
		switch (link.getLinkType())
		{
			case DESIGN_INSIDE:
				removeBadges(link);
				break;
			default:
				removeTokens(link);
				break;
		}
	}
	
	private void addBadges(CCanvasLink link)
	{
		addBadge(link.getAnchorA());
		addBadge(link.getAnchorB());
	}
	
	private void addBadge(CCanvasLinkAnchor anchor)
	{
		CCanvasLinkBadge badge = new CCanvasLinkBadge(anchor);
		badgesByAnchorId.put(badge.getLink().getId(), badge);
		getBadgesByCanvasId(badge.getCanvasUID()).add(badge.getLink().getId());
	}
	
	public CCanvasLinkBadge getBadgeByAnchorId(long anchor_uuid)
	{
		return badgesByAnchorId.get(anchor_uuid);
	}

	private void removeBadges(CCanvasLink link)
	{
		removeBadge(link.getAnchorA());
		removeBadge(link.getAnchorB());
	}
	
	private void removeBadge(CCanvasLinkAnchor anchor)
	{
		CCanvasLinkBadge badge = badgesByAnchorId.remove(anchor.getId());
		getBadgesByCanvasId(badge.getCanvasUID()).remove(badge.getLink().getId());

		if (anchor.getCanvasId() == currentCanvasId)
		{
			if (anchor.getLink().getAnchorA() == anchor)
			{
				outgoingLinkBay.refreshLayout();
			}
			else
			{
				incomingLinkBay.refreshLayout();
			}
		}
	}

	private List<Long> getBadgesByCanvasId(long canvas_uuid)
	{
		List<Long> badges = badgeAnchorIdsByCanvasId.get(canvas_uuid);
		if (badges == null)
		{
			badges = new ArrayList<Long>();
			badgeAnchorIdsByCanvasId.put(canvas_uuid, badges);
		}
		return badges;
	}

	private void addTokens(CCanvasLink link)
	{
		addToken(link.getAnchorA());
		addToken(link.getAnchorB());
	}
	
	private void addToken(CCanvasLinkAnchor anchor)
	{
		CCanvasLinkToken token = new CCanvasLinkToken(anchor);
		tokensByAnchorId.put(token.getLink().getId(), token);
		getTokensByCanvasId(token.getLink().getCanvasId()).add(token.getLink().getId());
		
		if (anchor.getCanvasId() == currentCanvasId)
		{
			if (anchor.getLink().getAnchorA() == anchor)
			{
				outgoingLinkBay.refreshLayout();
			}
			else
			{
				incomingLinkBay.refreshLayout();
			}
		}
	}

	private void removeTokens(CCanvasLink link)
	{
		removeToken(link.getAnchorA());
		removeToken(link.getAnchorB());
	}
	
	private void removeToken(CCanvasLinkAnchor anchor)
	{
		CCanvasLinkToken token = tokensByAnchorId.remove(anchor.getId());
		getTokensByCanvasId(token.getLink().getCanvasId()).remove(token.getLink().getId());

		if (anchor.getCanvasId() == currentCanvasId)
		{
			if (anchor.getLink().getAnchorA() == anchor)
			{
				outgoingLinkBay.refreshLayout();
			}
			else
			{
				incomingLinkBay.refreshLayout();
			}
		}
	}

	private List<Long> getTokensByCanvasId(long canvas_uuid)
	{
		List<Long> tokens = tokenAnchorIdsByCanvasId.get(canvas_uuid);
		if (tokens == null)
		{
			tokens = new ArrayList<Long>();
			tokenAnchorIdsByCanvasId.put(canvas_uuid, tokens);
		}
		return tokens;
	}
	
	public int getTokenCount(long canvas_uuid)
	{
		return getTokensByCanvasId(canvas_uuid).size();
	}

	public int getTokenCount(long canvas_uuid, CCanvasLink.LinkDirection direction)
	{
		int count = 0;
		for (long tokenAnchorId : getTokensByCanvasId(canvas_uuid))
		{
			CCanvasLinkToken token = tokensByAnchorId.get(tokenAnchorId);
			if (token.getDirection() == direction)
			{
				count++;
			}
		}
		return count;
	}

	public void populateTokens(long canvas_uuid, CCanvasLink.LinkDirection direction, List<CCanvasLinkToken> tokens)
	{
		tokens.clear();

		for (Long tokenAnchorId : getTokensByCanvasId(canvas_uuid))
		{
			CCanvasLinkToken token = this.tokensByAnchorId.get(tokenAnchorId);
			if (token.getDirection() == direction)
			{
				tokens.add(token);
			}
		}
		Collections.sort(tokens, sorter);
	}

	public void setSorter(Comparator<CCanvasLinkToken> sorter)
	{
		this.sorter = sorter;
	}

	public void canvasChanged(long canvas_uuid)
	{
		currentCanvasId = canvas_uuid;
		CanvasIntentionToolBar.getInstance().moveTo(canvas_uuid);

		incomingLinkBay.moveTo(canvas_uuid);
		outgoingLinkBay.moveTo(canvas_uuid);
	}

	public CanvasLinkBay getIncomingLinkBay()
	{
		return incomingLinkBay;
	}

	public CanvasLinkBay getOutgoingLinkBay()
	{
		return outgoingLinkBay;
	}

	private class UpperLeftLayout implements CanvasLinkBay.Layout
	{
		@Override
		public void updateBounds(Rectangle2D bounds, double width, double height)
		{
			bounds.setRect(CanvasLinkBay.BAY_INSET_X, CanvasLinkBay.BAY_INSET_Y, width, height);
		}
	}

	private class LowerRightLayout implements CanvasLinkBay.Layout
	{
		@Override
		public void updateBounds(Rectangle2D bounds, double width, double height)
		{
			double x = CalicoDataStore.ScreenWidth - (CanvasLinkBay.BAY_INSET_X + width);
			double y = CalicoDataStore.ScreenHeight - (CanvasLinkBay.BAY_INSET_Y + height);
			bounds.setRect(x, y, width, height);
		}
	}

	private class DefaultSorter implements Comparator<CCanvasLinkToken>
	{
		@Override
		public int compare(CCanvasLinkToken first, CCanvasLinkToken second)
		{
			int comparison = first.getLink().getType().compareTo(second.getLink().getType());
			if (comparison == 0)
			{
				return (int) (first.getLink().getCanvasId() - second.getLink().getCanvasId());
			}
			return comparison;
		}
	}
}