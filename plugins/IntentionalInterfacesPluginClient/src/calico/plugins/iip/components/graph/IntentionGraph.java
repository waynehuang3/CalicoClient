package calico.plugins.iip.components.graph;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;

import javax.swing.JComponent;

import calico.Calico;
import calico.CalicoDataStore;
import calico.components.grid.CGrid;
import calico.components.menus.CanvasMenuBar;
import calico.input.CalicoMouseListener;
import calico.inputhandlers.CalicoInputManager;
import calico.inputhandlers.InputEventInfo;
import calico.plugins.iip.components.menus.IntentionGraphMenuBar;
import calico.plugins.iip.components.menus.IntentionGraphZoomSlider;
import calico.plugins.iip.inputhandlers.IntentionGraphInputHandler;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PBounds;

public class IntentionGraph
{
	public enum Layer
	{
		CONTENT(0),
		TOOLS(1);

		public final int id;

		private Layer(int id)
		{
			this.id = id;
		}
	}

	public static IntentionGraph getInstance()
	{
		if (INSTANCE == null)
		{
			new IntentionGraph();
		}
		return INSTANCE;
	}

	private static IntentionGraph INSTANCE;

	private final PLayer toolLayer = new PLayer();

	private final ContainedCanvas canvas = new ContainedCanvas();
	private final ContainedCanvas contentCanvas = new ContainedCanvas();
	private IntentionGraphMenuBar menuBar;
	
	private boolean iconifyMode = false;

	private final long uuid;

	private IntentionGraph()
	{
		INSTANCE = this;

		uuid = Calico.uuid();

		// IntentionGraph.exitButtonBounds = new Rectangle(CalicoDataStore.ScreenWidth-32,5,24,24);

		canvas.setPreferredSize(new Dimension(CalicoDataStore.ScreenWidth, CalicoDataStore.ScreenHeight));
		setBounds(0, 0, CalicoDataStore.ScreenWidth, CalicoDataStore.ScreenHeight);
		contentCanvas.getLayer().translate(CalicoDataStore.ScreenWidth / 2, CalicoDataStore.ScreenHeight / 2);		

		CalicoInputManager.addCustomInputHandler(uuid, new IntentionGraphInputHandler());

		canvas.addMouseListener(new CalicoMouseListener());
		canvas.addMouseMotionListener(new CalicoMouseListener());

		canvas.removeInputEventListener(canvas.getPanEventHandler());
		canvas.removeInputEventListener(canvas.getZoomEventHandler());

		repaint();

		PLayer contentLayer = contentCanvas.getLayer();
		toolLayer.setParent(contentLayer.getParent());
		canvas.getCamera().addLayer(Layer.CONTENT.id, contentLayer);
		canvas.getCamera().addLayer(Layer.TOOLS.id, toolLayer);

		drawMenuBar();
	}

	public long getId()
	{
		return uuid;
	}

	public PLayer getLayer(Layer layer)
	{
		switch (layer)
		{
			case CONTENT:
				return contentCanvas.getLayer();
			case TOOLS:
				return toolLayer;
			default:
				throw new IllegalArgumentException("Unknown layer " + layer);
		}
	}

	public JComponent getComponent()
	{
		return canvas;
	}
	
	public void activateIconifyMode(boolean b)
	{
		iconifyMode = b;
	}

	public boolean getIconifyMode()
	{
		return iconifyMode;
	}
	
	public void fitContents()
	{
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;

		int visibleCount = 0;
		for (PNode node : (Iterable<PNode>) contentCanvas.getLayer().getChildrenReference())
		{
			if (node.getVisible())
			{
				visibleCount++;

				PBounds bounds = node.getBounds();
				if (bounds.x < minX)
				{
					minX = bounds.x;
				}
				if (bounds.y < minY)
				{
					minY = bounds.y;
				}
				if ((bounds.x + bounds.width) > maxX)
				{
					maxX = bounds.x + bounds.width;
				}
				if ((bounds.y + bounds.height) > maxY)
				{
					maxY = bounds.y + bounds.height;
				}
			}
		}

		if (visibleCount < 2)
		{
			contentCanvas.getLayer().setGlobalTranslation(new Point2D.Double(minX, minY));
		}
		else
		{
			Dimension canvasSize = contentCanvas.getBounds().getSize();
			double xRatio = canvasSize.width / (maxX - minX);
			double yRatio = canvasSize.height / (maxY - minY);

			double scale = Math.min(xRatio, yRatio) * 0.9;
			contentCanvas.getLayer().setScale(scale);
			double contentWidth = maxX - minX;
			double contentHeight = maxY - minY;
			double xMargin = (contentWidth * (xRatio - scale)) / 2;
			double yMargin = (contentHeight * (yRatio - scale)) / 2;

			contentCanvas.getLayer().setGlobalTranslation(new Point2D.Double(xMargin - minX, yMargin - minY));
		}

		repaint();
	}
	
	public void initialize()
	{
		menuBar.initialize();
	}

	public void repaint()
	{
		canvas.repaint();
		contentCanvas.repaint();
	}

	public Rectangle getBounds()
	{
		return canvas.getBounds();
	}

	public Rectangle getLocalBounds(Layer layer)
	{
		Rectangle globalBounds = canvas.getBounds();
		Point2D localPoint = getLayer(layer).globalToLocal(globalBounds.getLocation());
		Dimension2D localSize = getLayer(layer).globalToLocal(globalBounds.getSize());
		return new Rectangle((int) localPoint.getX(), (int) localPoint.getY(), (int) localSize.getWidth(), (int) localSize.getHeight());
	}

	public void setBounds(int x, int y, int w, int h)
	{
		canvas.setBounds(x, y, w, h);
	}

	private void drawMenuBar()
	{
		if (menuBar != null)
		{
			canvas.getCamera().removeChild(menuBar);
		}

		menuBar = new IntentionGraphMenuBar(CanvasMenuBar.POSITION_BOTTOM);
		canvas.getCamera().addChild(menuBar);

		contentCanvas.setBounds(0, 0, CalicoDataStore.ScreenWidth, (int) (CalicoDataStore.ScreenHeight - menuBar.getBounds().height));
	}

	public boolean processToolEvent(InputEventInfo event)
	{
		if (menuBar.isPointInside(event.getGlobalPoint()))
		{
			menuBar.processEvent(event);
			return true;
		}
		return false;
	}

	public void addMouseListener(MouseListener listener)
	{
		canvas.addMouseListener(listener);
	}

	public void addMouseMotionListener(MouseMotionListener listener)
	{
		canvas.addMouseMotionListener(listener);
	}

	public void removeMouseListener(MouseListener listener)
	{
		canvas.removeMouseListener(listener);
	}

	public void removeMouseMotionListener(MouseMotionListener listener)
	{
		canvas.removeMouseMotionListener(listener);
	}

	private class ContainedCanvas extends PCanvas
	{
		public ContainedCanvas()
		{
			super.removeInputSources();
		}
	}
}
