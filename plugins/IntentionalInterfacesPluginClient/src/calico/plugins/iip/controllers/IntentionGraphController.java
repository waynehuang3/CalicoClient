package calico.plugins.iip.controllers;

import it.unimi.dsi.fastutil.longs.Long2ReferenceArrayMap;

import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.util.List;

import calico.Calico;
import calico.Geometry;
import calico.components.menus.GridBottomMenuBar;
import calico.controllers.CCanvasController;
import calico.networking.Networking;
import calico.networking.PacketHandler;
import calico.networking.netstuff.CalicoPacket;
import calico.plugins.iip.IntentionalInterfacesNetworkCommands;
import calico.plugins.iip.components.CCanvasLink;
import calico.plugins.iip.components.CCanvasLinkAnchor;
import calico.plugins.iip.components.CCanvasLinkAnchor.ArrowEndpointType;
import calico.plugins.iip.components.CCanvasLinkArrow;
import calico.plugins.iip.components.CIntentionCell;
import calico.plugins.iip.components.graph.IntentionGraph;
import calico.plugins.iip.components.graph.ShowIntentionGraphButton;
import edu.umd.cs.piccolo.util.PBounds;

public class IntentionGraphController
{
	private enum CellEdge
	{
		TOP
		{
			@Override
			boolean findIntersection(PBounds cellBounds, double xOpposite, double yOpposite, double[] intersection)
			{
				int result = Geometry.findLineSegmentIntersection(cellBounds.getX(), cellBounds.getY(), cellBounds.getX() + cellBounds.getWidth(),
						cellBounds.getY(), cellBounds.getCenterX(), cellBounds.getCenterY(), xOpposite, yOpposite, intersection);
				return result == 1;
			}
		},
		RIGHT
		{
			@Override
			boolean findIntersection(PBounds cellBounds, double xOpposite, double yOpposite, double[] intersection)
			{
				int result = Geometry.findLineSegmentIntersection(cellBounds.getX(), cellBounds.getY() + cellBounds.getHeight(),
						cellBounds.getX() + cellBounds.getWidth(), cellBounds.getY() + cellBounds.getHeight(), cellBounds.getCenterX(),
						cellBounds.getCenterY(), xOpposite, yOpposite, intersection);
				return result == 1;
			}
		},
		BOTTOM
		{
			@Override
			boolean findIntersection(PBounds cellBounds, double xOpposite, double yOpposite, double[] intersection)
			{
				int result = Geometry.findLineSegmentIntersection(cellBounds.getX(), cellBounds.getY(), cellBounds.getX(),
						cellBounds.getY() + cellBounds.getHeight(), cellBounds.getCenterX(), cellBounds.getCenterY(), xOpposite, yOpposite, intersection);
				return result == 1;
			}
		},
		LEFT
		{
			@Override
			boolean findIntersection(PBounds cellBounds, double xOpposite, double yOpposite, double[] intersection)
			{
				int result = Geometry.findLineSegmentIntersection(cellBounds.getX() + cellBounds.getWidth(), cellBounds.getY(),
						cellBounds.getX() + cellBounds.getWidth(), cellBounds.getY() + cellBounds.getHeight(), cellBounds.getCenterX(),
						cellBounds.getCenterY(), xOpposite, yOpposite, intersection);
				return result == 1;
			}
		};

		abstract boolean findIntersection(PBounds cellBounds, double xOpposite, double yOpposite, double[] intersection);
	}

	public static IntentionGraphController getInstance()
	{
		return INSTANCE;
	}

	public static void initialize()
	{
		INSTANCE = new IntentionGraphController();

		GridBottomMenuBar.addMenuButtonRightAligned(ShowIntentionGraphButton.class);
	}

	private static IntentionGraphController INSTANCE;

	private final Long2ReferenceArrayMap<CCanvasLinkArrow> arrowsByLinkId = new Long2ReferenceArrayMap<CCanvasLinkArrow>();

	public void addLink(CCanvasLink link)
	{
		CCanvasLinkArrow arrow = new CCanvasLinkArrow(link);
		arrowsByLinkId.put(link.getId(), arrow);
		IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).addChild(arrow);
		arrow.redraw();
	}
	
	public CCanvasLinkArrow getArrowByLinkId(long uuid)
	{
		return arrowsByLinkId.get(uuid);
	}

	public Point2D alignCellEdgeAtLinkEndpoint(long linkSourceCanvasId, double xLinkEndpoint, double yLinkEndpoint)
	{
		CIntentionCell sourceCell = CIntentionCellController.getInstance().getCellByCanvasId(linkSourceCanvasId);
		Point2D sourceCenter = sourceCell.getCenter();

		Dimension2D cellSize = sourceCell.getSize();
		Point2D.Double cellOrigin = new Point2D.Double();
		double xDelta = (xLinkEndpoint - sourceCenter.getX());
		if (Math.abs(xDelta) < 0.01)
		{
			if (yLinkEndpoint < sourceCenter.getY())
			{
				cellOrigin.setLocation(xLinkEndpoint - (cellSize.getWidth() / 2.0), yLinkEndpoint - sourceCell.getSize().getHeight());
			}
			else
			{
				cellOrigin.setLocation(xLinkEndpoint - (cellSize.getWidth() / 2.0), yLinkEndpoint);
			}
		}
		else
		{
			double cellDiagonalSlope = (cellSize.getHeight() / cellSize.getWidth());
			double linkSlope = (yLinkEndpoint - sourceCenter.getY()) / xDelta;

			if (Math.abs(linkSlope) < cellDiagonalSlope)
			{ // attaching to right or left edge
				double xSpanToCenter = (cellSize.getWidth() / 2.0);
				double ySpanToCenter = (xSpanToCenter * linkSlope);
				if (xLinkEndpoint > sourceCenter.getX())
				{ // left
					cellOrigin.setLocation(xLinkEndpoint, yLinkEndpoint + ySpanToCenter - (cellSize.getHeight() / 2.0));
				}
				else
				{ // right
					cellOrigin.setLocation(xLinkEndpoint - cellSize.getWidth(), yLinkEndpoint - (ySpanToCenter + (cellSize.getHeight() / 2.0)));
				}
			}
			else
			{ // top/bottom
				double ySpanToCenter = (cellSize.getHeight() / 2.0);
				double xSpanToCenter = (ySpanToCenter / linkSlope);
				if (yLinkEndpoint > sourceCenter.getY())
				{ // top
					cellOrigin.setLocation(xLinkEndpoint + xSpanToCenter - (cellSize.getWidth() / 2.0), yLinkEndpoint);
				}
				else
				{ // bottom
					cellOrigin.setLocation(xLinkEndpoint - (xSpanToCenter + (cellSize.getWidth() / 2.0)), yLinkEndpoint - cellSize.getHeight());
				}
			}
		}

		return cellOrigin;
	}

	private void updateAnchorPosition(CCanvasLinkAnchor anchor)
	{
		if (anchor.getArrowEndpointType() == ArrowEndpointType.INTENTION_CELL)
		{
			CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(anchor.getCanvasId());

			Point2D position = alignAnchorAtCellEdge(cell.getLocation().getX(), cell.getLocation().getY(), cell.getSize(), getOppositePosition(anchor));
			anchor.getPoint().setLocation(position);
		}
	}

	private Point2D getOppositePosition(CCanvasLinkAnchor anchor)
	{
		CCanvasLinkAnchor opposite = anchor.getOpposite();
		switch (opposite.getArrowEndpointType())
		{
			case INTENTION_CELL:
				return CIntentionCellController.getInstance().getCellByCanvasId(opposite.getCanvasId()).getCenter();
			case FLOATING:
				return anchor.getPoint();
			default:
				throw new IllegalArgumentException("Unknown anchor type " + anchor.getArrowEndpointType());
		}
	}

	public void removeLink(CCanvasLink link)
	{
		CCanvasLinkArrow arrow = arrowsByLinkId.remove(link.getId());
		IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).removeChild(arrow);
	}

	public void contentChanged(long canvas_uuid)
	{
		CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(canvas_uuid);
		if (cell == null)
		{
			return;
		}

		cell.contentsChanged();
		IntentionGraph.getInstance().repaint();
	}

	public void initializeDisplay()
	{
		IntentionGraph.getInstance().initialize();
	}

	public void updateLinkArrow(CCanvasLink link)
	{
		CCanvasLinkArrow arrow = arrowsByLinkId.get(link.getId());
		alignAnchors(link);
		arrow.redraw();
	}

	public void localUpdateAttachedArrows(long cellId, double x, double y)
	{
		CIntentionCell cell = CIntentionCellController.getInstance().getCellById(cellId);
		long canvasId = cell.getCanvasId();
		List<Long> anchorIds = CCanvasLinkController.getInstance().getAnchorIdsByCanvasId(canvasId);
		for (long anchorId : anchorIds)
		{
			CCanvasLinkAnchor anchor = CCanvasLinkController.getInstance().getAnchor(anchorId);
			Point2D edgePosition = alignAnchorAtCellEdge(x, y, cell.getSize(), getOppositePosition(anchor));
			anchor.getPoint().setLocation(edgePosition);
			updateLinkArrow(anchor.getLink());
		}
	}

	public void updateAttachedArrows(long cellId, double x, double y)
	{
		CIntentionCell cell = CIntentionCellController.getInstance().getCellById(cellId);
		long canvasId = cell.getCanvasId();
		List<Long> anchorIds = CCanvasLinkController.getInstance().getAnchorIdsByCanvasId(canvasId);
		for (long anchorId : anchorIds)
		{
			CCanvasLinkAnchor anchor = CCanvasLinkController.getInstance().getAnchor(anchorId);
			Point2D edgePosition = alignAnchorAtCellEdge(x, y, cell.getSize(), getOppositePosition(anchor));

			CCanvasLinkController.getInstance().moveLinkAnchor(anchor, edgePosition);
		}
	}

	public Point2D getArrowAnchorPosition(long canvas_uuid, long opposite_canvas_uuid)
	{
		CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(opposite_canvas_uuid);
		return getArrowAnchorPosition(canvas_uuid, cell.getCenter());
	}

	public Point2D getArrowAnchorPosition(long canvas_uuid, Point2D opposite)
	{
		return getArrowAnchorPosition(canvas_uuid, opposite.getX(), opposite.getY());
	}

	public Point2D getArrowAnchorPosition(long canvas_uuid, double xOpposite, double yOpposite)
	{
		CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(canvas_uuid);
		return alignAnchorAtCellEdge(cell.copyBounds(), xOpposite, yOpposite);
	}

	private void alignAnchors(CCanvasLink link)
	{
		CIntentionCell fromCell = CIntentionCellController.getInstance().getCellByCanvasId(link.getAnchorA().getCanvasId());
		CIntentionCell toCell = CIntentionCellController.getInstance().getCellByCanvasId(link.getAnchorB().getCanvasId());
		Point2D aPosition;
		Point2D bPosition;

		if (fromCell == null)
		{
			aPosition = link.getAnchorA().getPoint();
		}
		else
		{
			if (toCell == null)
			{
				aPosition = alignAnchorAtCellEdge(fromCell.copyBounds(), link.getAnchorB().getPoint());
			}
			else
			{
				aPosition = alignAnchorAtCellEdge(fromCell.copyBounds(), toCell.getCenter());
			}
		}

		if (toCell == null)
		{
			bPosition = link.getAnchorB().getPoint();
		}
		else
		{
			if (fromCell == null)
			{
				bPosition = alignAnchorAtCellEdge(toCell.copyBounds(), link.getAnchorA().getPoint());
			}
			else
			{
				bPosition = alignAnchorAtCellEdge(toCell.copyBounds(), fromCell.getCenter());
			}
		}

		link.getAnchorA().getPoint().setLocation(aPosition);
		link.getAnchorB().getPoint().setLocation(bPosition);
	}

	private Point2D alignAnchorAtCellEdge(double xCell, double yCell, Dimension2D cellSize, Point2D opposite)
	{
		return alignAnchorAtCellEdge(new PBounds(xCell, yCell, cellSize.getWidth(), cellSize.getHeight()), opposite.getX(), opposite.getY());
	}

	private Point2D alignAnchorAtCellEdge(PBounds cellBounds, Point2D opposite)
	{
		return alignAnchorAtCellEdge(cellBounds, opposite.getX(), opposite.getY());
	}

	private Point2D alignAnchorAtCellEdge(PBounds cellBounds, double xOpposite, double yOpposite)
	{
		double[] intersection = new double[2];
		for (CellEdge edge : CellEdge.values())
		{
			if (edge.findIntersection(cellBounds, xOpposite, yOpposite, intersection))
			{
				return new Point2D.Double(intersection[0], intersection[1]);
			}
		}

		System.out.println("Failed to align an arrow to a CIntentionCell edge--can't find the arrow's intersection with the cell!");

		return new Point2D.Double(cellBounds.getCenterX(), cellBounds.getCenterY());
	}
}
