package BiranArtem_group6_lab5_var4a;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.swing.JPanel;

public class GraphicsDisplay extends JPanel {
    private ArrayList<Double[]> graphicsData;
    private ArrayList<Double[]> originalData;

    private int selectedMarker = -1;

    private double minX;
    private double maxX;
    private double minY;
    private double maxY;

    private double[][] viewport = new double[2][2];

    private ArrayList<double[][]> undoHistory = new ArrayList<>();

    private double scaleX;
    private double scaleY;

    private BasicStroke axisStroke;
    private BasicStroke gridStroke;
    private BasicStroke markerStroke;
    private BasicStroke selectionStroke;

    private Font axisFont;
    private Font labelsFont;

    private boolean scaleMode = false;
    private boolean changeMode = false;
    private boolean fill = false;

    private static DecimalFormat formatter = (DecimalFormat)NumberFormat.getInstance();

    private double[] originalPoint = new double[2];

    private Rectangle2D.Double selectionRect = new Rectangle2D.Double();

    public GraphicsDisplay() {
        setBackground(Color.WHITE);

        axisStroke = new BasicStroke(2.0F, 0, 0, 10.0F, null, 0.0F);
        gridStroke = new BasicStroke(1.0F, 0, 0, 10.0F, new float[] { 4.0F, 4.0F }, 0.0F);
        markerStroke = new BasicStroke(1.0F, 0, 0, 10.0F, null, 0.0F);
        selectionStroke = new BasicStroke(1.0F, 0, 0, 10.0F, new float[] { 10.0F, 10.0F }, 0.0F);
        axisFont = new Font("Serif", 1, 36);
        labelsFont = new Font("Serif", 0, 10);

        addMouseListener(new MouseHandler());
        addMouseMotionListener(new MouseMotionHandler());
    }

    public void setFill(boolean fill){
        this.fill = fill;
        repaint();
    }

    public void displayGraphics(ArrayList<Double[]> graphicsData) {
        this.graphicsData = graphicsData;
        originalData = new ArrayList<>(graphicsData.size());
        for (Double[] point : graphicsData) {
            originalData.add(point.clone());
        }

        minX = graphicsData.get(0)[0];
        maxX = graphicsData.get(graphicsData.size() - 1)[0];
        minY = graphicsData.get(0)[1];
        maxY = minY;

        for (int i = 1; i < graphicsData.size(); i++) {
            if (graphicsData.get(i)[1] < minY)
                minY = graphicsData.get(i)[1];
            if (graphicsData.get(i)[1] > maxY)
                maxY = graphicsData.get(i)[1];
        }
        zoomToRegion(minX, maxY, maxX, minY);
    }

    public void zoomToRegion(double minX, double maxY, double maxX, double minY) {
        viewport[0][0] = minX;
        viewport[0][1] = maxY;
        viewport[1][0] = maxX;
        viewport[1][1] = minY;
        repaint();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        this.scaleX = getSize().getWidth() / (this.viewport[1][0] - this.viewport[0][0]);
        this.scaleY = getSize().getHeight() / (this.viewport[0][1] - this.viewport[1][1]);

        if (graphicsData == null || graphicsData.size() == 0)
            return;

        Graphics2D canvas = (Graphics2D)g;

        if (fill) fillGraphics(canvas);
        paintAxis(canvas);
        paintGraphics(canvas);
        paintMarkers(canvas);
        paintLabels(canvas);
        paintSelection(canvas);
    }

    private void fillGraphics(Graphics2D canvas){
        canvas.setStroke(axisStroke);
        canvas.setColor(Color.MAGENTA);
        canvas.setPaint(Color.MAGENTA);

        Point2D.Double point = translateXYtoPoint(minX,graphicsData.get(0)[1] );
        GeneralPath gr = new GeneralPath();
        gr.moveTo(point.getX(), point.getY());
        for (int i = 0; i < graphicsData.size(); i++) {
            point = translateXYtoPoint(graphicsData.get(i)[0], graphicsData.get(i)[1]);
            gr.lineTo(point.getX(), point.getY());
        }
        point = translateXYtoPoint(maxX, graphicsData.get(graphicsData.size()-1)[1]);
        gr.lineTo(point.getX(), point.getY());
        point = translateXYtoPoint(maxX, minY);
        gr.lineTo(point.getX(), point.getY());
        point =  translateXYtoPoint(minX, minY);
        gr.lineTo(point.getX(), point.getY());
        gr.closePath();

        canvas.fill(gr);
        canvas.draw(gr);
    }

    private void paintSelection(Graphics2D canvas) {
        if (!scaleMode)
            return;
        canvas.setStroke(selectionStroke);
        canvas.setColor(Color.BLACK);
        canvas.draw(selectionRect);
    }

    private void paintGraphics(Graphics2D canvas) {
        canvas.setStroke(markerStroke);
        canvas.setColor(Color.RED);

        Double currentX = null;
        Double currentY = null;

        for (Double[] point : graphicsData) {
            if (point[0] < viewport[0][0] || point[1] > viewport[0][1] ||
                    point[0] > viewport[1][0] || point[1] < viewport[1][1])
                continue;
            if (currentX != null && currentY != null)
                canvas.draw(new Line2D.Double(translateXYtoPoint(currentX, currentY),
                        translateXYtoPoint(point[0], point[1])));
            currentX = point[0];
            currentY = point[1];
        }
    }

    private void paintMarkers(Graphics2D canvas) {
        canvas.setStroke(this.markerStroke);
        canvas.setColor(Color.RED);
        canvas.setPaint(Color.RED);

        Ellipse2D.Double lastMarker = null;
        int i = -1;
        for (Double[] point : graphicsData) {
            int radius;
            i++;
            if (point[0] < viewport[0][0] || point[1] > viewport[0][1] ||
                    point[0] > viewport[1][0] || point[1] < viewport[1][1])
                continue;
            if (i == selectedMarker) {
                radius = 6;
            } else {
                radius = 3;
            }
            Ellipse2D.Double marker = new Ellipse2D.Double();
            Point2D center = translateXYtoPoint(point[0], point[1]);
            Point2D corner = new Point2D.Double(center.getX() + radius, center.getY() + radius);
            marker.setFrameFromCenter(center, corner);
            if (i == this.selectedMarker) {
                lastMarker = marker;
                continue;
            }
            canvas.draw(marker);
            canvas.fill(marker);
        }
        if (lastMarker != null) {
            canvas.setColor(Color.BLUE);
            canvas.setPaint(Color.BLUE);
            canvas.draw(lastMarker);
            canvas.fill(lastMarker);
        }
    }

    private void paintLabels(Graphics2D canvas) {
        canvas.setColor(Color.BLACK);
        canvas.setFont(labelsFont);

        double labelXPos, labelYPos;

        FontRenderContext context = canvas.getFontRenderContext();
        if (viewport[1][1] < 0.0D && viewport[0][1] > 0.0D) {
            labelYPos = 0.0D;
        } else {
            labelYPos = viewport[1][1];
        }
        if (viewport[0][0] < 0.0D && viewport[1][0] > 0.0D) {
            labelXPos = 0.0D;
        } else {
            labelXPos = viewport[0][0];
        }
        double pos = viewport[0][0];
        double step = (viewport[1][0] - viewport[0][0]) / 10.0D;
        while (pos < viewport[1][0]) {
            Point2D.Double point = translateXYtoPoint(pos, labelYPos);
            String label = formatter.format(pos);
            Rectangle2D bounds = labelsFont.getStringBounds(label, context);
            canvas.drawString(label, (float)(point.getX() + 5.0D), (float)(point.getY() - bounds.getHeight()));
            pos += step;
        }
        pos = viewport[1][1];
        step = (viewport[0][1] - viewport[1][1]) / 10.0D;
        while (pos < viewport[0][1]) {
            Point2D.Double point = translateXYtoPoint(labelXPos, pos);
            String label = formatter.format(pos);
            Rectangle2D bounds = labelsFont.getStringBounds(label, context);
            canvas.drawString(label, (float)(point.getX() + 5.0D), (float)(point.getY() - bounds.getHeight()));
            pos += step;
        }
        if (selectedMarker >= 0) {
            Point2D.Double point = translateXYtoPoint(graphicsData.get(selectedMarker)[0], graphicsData.get(selectedMarker)[1]);
            String label = "X=" + formatter.format(graphicsData.get(selectedMarker)[0]) + ", Y=" + formatter.format(graphicsData.get(selectedMarker)[1]);
            Rectangle2D bounds = labelsFont.getStringBounds(label, context);
            canvas.setColor(Color.BLUE);
            canvas.drawString(label, (float)(point.getX() + 5.0D), (float)(point.getY() - bounds.getHeight()));
        }
    }

    private void paintAxis(Graphics2D canvas) {
        canvas.setStroke(axisStroke);
        canvas.setColor(Color.BLACK);
        canvas.setFont(axisFont);

        FontRenderContext context = canvas.getFontRenderContext();
        if (viewport[0][0] <= 0.0D && viewport[1][0] >= 0.0D) {
            canvas.draw(new Line2D.Double(translateXYtoPoint(0.0D, viewport[0][1]),
                    translateXYtoPoint(0.0D, viewport[1][1])));
            canvas.draw(new Line2D.Double(translateXYtoPoint(-(viewport[1][0] - viewport[0][0]) * 0.0025D, viewport[0][1] - (viewport[0][1] - viewport[1][1]) * 0.015D),
                    translateXYtoPoint(0.0D, viewport[0][1])));
            canvas.draw(new Line2D.Double(translateXYtoPoint((viewport[1][0] - viewport[0][0]) * 0.0025D, viewport[0][1] - (viewport[0][1] - viewport[1][1]) * 0.015D),
                    translateXYtoPoint(0.0D, viewport[0][1])));
            Rectangle2D bounds = axisFont.getStringBounds("y", context);
            Point2D.Double labelPos = translateXYtoPoint(0.0D, viewport[0][1]);
            canvas.drawString("y", (float)labelPos.x + 10.0F, (float)(labelPos.y + bounds.getHeight() / 2.0D));
        }
        if (viewport[1][1] <= 0.0D && viewport[0][1] >= 0.0D) {
            canvas.draw(new Line2D.Double(translateXYtoPoint(viewport[0][0], 0.0D),
                    translateXYtoPoint(viewport[1][0], 0.0D)));
            canvas.draw(new Line2D.Double(translateXYtoPoint(viewport[1][0] - (viewport[1][0] - viewport[0][0]) * 0.01D, (viewport[0][1] - viewport[1][1]) * 0.005D),
                    translateXYtoPoint(viewport[1][0], 0.0D)));
            canvas.draw(new Line2D.Double(translateXYtoPoint(viewport[1][0] - (viewport[1][0] - viewport[0][0]) * 0.01D, -(viewport[0][1] - viewport[1][1]) * 0.005D),
                    translateXYtoPoint(viewport[1][0], 0.0D)));
            Rectangle2D bounds = axisFont.getStringBounds("x", context);
            Point2D.Double labelPos = translateXYtoPoint(viewport[1][0], 0.0D);
            canvas.drawString("x", (float)(labelPos.x - bounds.getWidth() - 10.0D), (float)(labelPos.y - bounds.getHeight() / 2.0D));
        }
    }

    protected Point2D.Double translateXYtoPoint(double x, double y) {
        double deltaX = x - viewport[0][0];
        double deltaY = viewport[0][1] - y;
        return new Point2D.Double(deltaX * scaleX, deltaY * scaleY);
    }

    protected double[] translatePointToXY(int x, int y) {
        return new double[] { viewport[0][0] + x / scaleX, viewport[0][1] - y / scaleY };
    }

    protected int findSelectedPoint(int x, int y) {
        if (graphicsData == null)
            return -1;
        int pos = 0;
        for (Double[] point : graphicsData) {
            Point2D.Double screenPoint = translateXYtoPoint(point[0], point[1]);
            double distance = (screenPoint.getX() - x) * (screenPoint.getX() - x) + (screenPoint.getY() - y) * (screenPoint.getY() - y);
            if (distance < 100.0D)
                return pos;
            pos++;
        }
        return -1;
    }

    public void reset() {
        displayGraphics(originalData);
    }

    public class MouseHandler extends MouseAdapter {
        public void mouseClicked(MouseEvent ev) {
            if (ev.getButton() == 3) {
                if (undoHistory.size() > 0) {
                    viewport = undoHistory.get(undoHistory.size() - 1);
                    undoHistory.remove(undoHistory.size() - 1);
                } else {
                    zoomToRegion(minX, maxY, maxX, minY);
                }
                repaint();
            }
        }

        public void mousePressed(MouseEvent ev) {
            if (ev.getButton() != 1)
                return;
            selectedMarker = findSelectedPoint(ev.getX(), ev.getY());
            originalPoint = translatePointToXY(ev.getX(), ev.getY());
            if (selectedMarker >= 0) {
                changeMode = true;
                setCursor(Cursor.getPredefinedCursor(8));
            } else {
                scaleMode = true;
                setCursor(Cursor.getPredefinedCursor(5));
                selectionRect.setFrame(ev.getX(), ev.getY(), 1.0D, 1.0D);
            }
        }

        public void mouseReleased(MouseEvent ev) {
            if (ev.getButton() != 1)
                return;
            setCursor(Cursor.getPredefinedCursor(0));
            if (changeMode) {
                changeMode = false;
            } else {
                scaleMode = false;
                double[] finalPoint = translatePointToXY(ev.getX(), ev.getY());
                undoHistory.add(viewport);
                viewport = new double[2][2];
                zoomToRegion(originalPoint[0], originalPoint[1], finalPoint[0], finalPoint[1]);
                repaint();
            }
        }
    }

    public class MouseMotionHandler implements MouseMotionListener {
        public void mouseMoved(MouseEvent ev) {
            selectedMarker = findSelectedPoint(ev.getX(), ev.getY());
            if (selectedMarker >= 0) {
                setCursor(Cursor.getPredefinedCursor(8));
            } else {
                setCursor(Cursor.getPredefinedCursor(0));
            }
            repaint();
        }

        public void mouseDragged(MouseEvent ev) {
            if (changeMode) {
                double[] currentPoint = translatePointToXY(ev.getX(), ev.getY());
                double newY = graphicsData.get(selectedMarker)[1] + currentPoint[1] - graphicsData.get(selectedMarker)[1];
                if (newY > viewport[0][1])
                    newY = viewport[0][1];
                if (newY < viewport[1][1])
                    newY = viewport[1][1];
                graphicsData.get(selectedMarker)[1] = newY;
                repaint();
            } else {
                double width = ev.getX() - selectionRect.getX();
                if (width < 5.0D)
                    width = 5.0D;
                double height = ev.getY() - selectionRect.getY();
                if (height < 5.0D)
                    height = 5.0D;
                selectionRect.setFrame(selectionRect.getX(),selectionRect.getY(), width, height);
                repaint();
            }
        }
    }
}