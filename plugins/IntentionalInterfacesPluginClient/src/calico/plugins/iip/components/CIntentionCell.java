package calico.plugins.iip.components;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import calico.controllers.CCanvasController;
import calico.plugins.iip.components.graph.IntentionGraph;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.iconsets.CalicoIconManager;
import calico.plugins.iip.util.IntentionalInterfacesGraphics;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PImage;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolox.nodes.PComposite;

public class CIntentionCell
{
	private static final double MINIMUM_SNAPSHOT_SCALE = 1.0;

	private enum BorderColor
	{
		PLAIN(Color.black),
		HIGHLIGHTED(new Color(0xFFFF30));

		Color color;

		private BorderColor(Color color)
		{
			this.color = color;
		}
	}

	long uuid;
	long canvas_uuid;
	Point2D location;

	private boolean highlighted = false;

	private final Shell shell;

	public CIntentionCell(long uuid, long canvas_uuid, double x, double y)
	{
		this.uuid = uuid;
		this.canvas_uuid = canvas_uuid;
		this.location = new Point2D.Double(x, y);

		shell = new Shell(x, y);

		IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).addChild(shell);
	}

	private Color currentBorderColor()
	{
		if (highlighted)
		{
			return BorderColor.HIGHLIGHTED.color;
		}
		else
		{
			return BorderColor.PLAIN.color;
		}
	}

	public long getId()
	{
		return uuid;
	}

	public long getCanvasId()
	{
		return canvas_uuid;
	}

	public boolean contains(Point2D point)
	{
		PBounds bounds = shell.getGlobalBounds();
		return ((point.getX() > bounds.x) && (point.getY() > bounds.y) && ((point.getX() - bounds.x) < bounds.width) && (point.getY() - bounds.y) < bounds.height);
	}

	public Point2D getLocation()
	{
		return shell.getBounds().getOrigin();
	}

	public void setLocation(double x, double y)
	{
		System.out.println("Move cell " + CCanvasController.canvasdb.get(canvas_uuid).getGridCoordTxt() + " to " + x + ", " + y);

		location.setLocation(x, y);
		shell.setBounds(x, y, shell.getBounds().getWidth(), shell.getBounds().getHeight());

		shell.repaint();
	}

	public boolean isVisible()
	{
		return shell.getVisible();
	}

	public void setVisible(boolean b)
	{
		shell.setVisible(b);

		System.out.println((b ? "Showing" : "Hiding") + " a CIC: " + CIntentionCellController.getInstance().listVisibleCellAddresses());
	}

	public void setHighlighted(boolean highlighted)
	{
		this.highlighted = highlighted;
		shell.repaint();
	}

	public boolean isInGraphFootprint()
	{
		return IntentionGraph.getInstance().getBounds().intersects(shell.getBounds());
	}

	public void contentsChanged()
	{
		shell.canvasSnapshot.contentsChanged();
	}

	private boolean scaleAllowsSnapshot()
	{
		return IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).getScale() >= MINIMUM_SNAPSHOT_SCALE;
	}

	private static final int BORDER_WIDTH = 1;

	private class Shell extends PComposite implements PropertyChangeListener
	{
		private final PImage canvasAddress;
		private final CanvasSnapshot canvasSnapshot = new CanvasSnapshot();

		private boolean showingSnapshot = false;

		private double lastScale = Double.MIN_VALUE;

		public Shell(double x, double y)
		{
			canvasAddress = new PImage(IntentionalInterfacesGraphics.superimposeCellAddress(
					CalicoIconManager.getIconImage("intention-graph.obscured-intention-cell"), canvas_uuid));

			addChild(canvasAddress);
			setBounds(x, y, canvasAddress.getWidth() + (2 * BORDER_WIDTH), canvasAddress.getHeight() + (2 * BORDER_WIDTH));
			// canvasAddress.setBounds(shell.getBounds());
			repaint();

			IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).addPropertyChangeListener(PNode.PROPERTY_TRANSFORM, this);
		}

		@Override
		public void propertyChange(PropertyChangeEvent event)
		{
			if (IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).getScale() != lastScale)
			{
				lastScale = IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).getScale();

				if (showingSnapshot != scaleAllowsSnapshot())
				{
					if (showingSnapshot)
					{
						removeChild(canvasSnapshot.snapshot);
						addChild(canvasAddress);
					}
					else
					{
						removeChild(canvasAddress);
						addChild(canvasSnapshot.snapshot);
					}

					showingSnapshot = !showingSnapshot;
				}
			}

			if (canvasSnapshot.isDirty)
			{
				canvasSnapshot.contentsChanged();
			}
		}

		@Override
		protected void paint(PPaintContext paintContext)
		{
			super.paint(paintContext);

			Graphics2D g = paintContext.getGraphics();
			Color c = g.getColor();
			PBounds bounds = getBounds();

			g.setColor(currentBorderColor());
			g.translate(bounds.x, bounds.y);
			g.drawRect(0, 0, ((int) bounds.width) - 1, ((int) bounds.height) - 1);
			IntentionalInterfacesGraphics.superimposeCellAddressInCorner(g, canvas_uuid, 16);

			g.translate(-bounds.x, -bounds.y);
			g.setColor(c);
		}

		@Override
		protected void layoutChildren()
		{
			PBounds bounds = getBounds();

			if (showingSnapshot)
			{
				canvasSnapshot.snapshot.setBounds(bounds.x + BORDER_WIDTH, bounds.y + BORDER_WIDTH, bounds.width - (2 * BORDER_WIDTH), bounds.height
						- (2 * BORDER_WIDTH));
			}
			else
			{
				canvasAddress
						.setBounds(bounds.x + BORDER_WIDTH, bounds.y + BORDER_WIDTH, bounds.width - (2 * BORDER_WIDTH), bounds.height - (2 * BORDER_WIDTH));
			}
		}
	}

	private class CanvasSnapshot
	{
		private final PImage snapshot = new PImage();

		private boolean isDirty = true;

		boolean isOnScreen()
		{
			return (isInGraphFootprint() && scaleAllowsSnapshot());
		}

		void contentsChanged()
		{
			// TODO: updateSnapshot() when dirty and dragged into view (will propertyChange() be triggered?)

			if (isVisible() && isInGraphFootprint() && shell.showingSnapshot)
			{
				updateSnapshot();
			}
			else
			{
				isDirty = true;
			}
		}

		private void updateSnapshot()
		{
			snapshot.setImage(CCanvasController.canvasdb.get(canvas_uuid).getContentCamera().toImage());
			snapshot.setBounds(shell.getBounds());
			isDirty = false;

			snapshot.repaint();
		}
	}
}
