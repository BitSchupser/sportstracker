package de.saring.exerciseviewer.gui.panels;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.border.EmptyBorder;

import org.jdesktop.swingx.JXMapKit;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.JXMapKit.DefaultProviders;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.painter.Painter;

import com.google.inject.Inject;

import de.saring.exerciseviewer.data.EVExercise;
import de.saring.exerciseviewer.data.ExerciseSample;
import de.saring.exerciseviewer.data.Position;
import de.saring.exerciseviewer.gui.EVContext;

/**
 * This class is the implementation of the "Track" panel, which displays the recorded location  
 * data of the exercise (if available) in a map.<br>
 * The map component is JXMapKit from the SwingLabs project, the data provider is OpenStreetMap.
 *
 * @author Stefan Saring
 * @version 1.0
 */
public class TrackPanel extends BasePanel {
    
    private JXMapKit mapKit;
    
    /**
     * Standard c'tor.
     * @param context the ExerciseViewer context
     */
    @Inject
    public TrackPanel (EVContext context) {
        super (context);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(8, 8, 8, 8));
        
        // create map viewer later, only when location data is available
    }
    
    /** {@inheritDoc} */
    @Override
    public void displayExercise () {

        // show track in mapviewer if data is available 
        final EVExercise exercise = getDocument ().getExercise ();
        if (exercise.getRecordingMode().isLocation()) {
            // TODO: is exception handling required? e.g. when there's no internet connection
            setupMapViewer();
            showTrack(exercise);
        }
        else {
            // TODO: show "no track data available" or a disabled map component
        }
    }
    
    private void setupMapViewer() {
        mapKit = new JXMapKit();
        mapKit.setDefaultProvider(DefaultProviders.OpenStreetMaps);           

        removeAll();
        add(mapKit, java.awt.BorderLayout.CENTER);  
    }
   
    /**
     * Displays the track of the specified exercise.
     * 
     * @param exercise the exercise with track data
     */
    private void showTrack(EVExercise exercise) {
        List<GeoPosition> geoPositions = createGeoPositionList(exercise);
        
        if (!geoPositions.isEmpty()) {
            // TODO: how to set proper center position and the fitting zoom factor?
            // set initial map position and zoom factor
            GeoPosition startPosition = geoPositions.get(0);
            mapKit.setCenterPosition(new GeoPosition(startPosition.getLatitude(), startPosition.getLongitude()));  
            mapKit.setZoom(5);  
            
            // display track
            setupTrackPainter(geoPositions);
        }
    }
    
    /**
     * Creates a custom painter which draws the track.
     * 
     * @param geoPositions list of GeoPosition objects of this track
     */
    private void setupTrackPainter(final List<GeoPosition> geoPositions) {
        // This code is based on the example from 
        // http://www.naxos-software.de/blog/index.php?/archives/92-TracknMash-Openstreetmap-Karten-in-JavaSwing-mit-JXMapViewer.html
        
        Painter<JXMapViewer> lineOverlay = new Painter<JXMapViewer>() {
            public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
                
                g = (Graphics2D) g.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // convert from viewport to world bitmap
                Rectangle rect = mapKit.getMainMap().getViewportBounds();
                g.translate(-rect.x, -rect.y);

                // draw track line and waypoints for start and end position
                drawTrackLine(g, geoPositions);
                drawWaypoint(g, geoPositions.get(0), Color.GREEN);
                drawWaypoint(g, geoPositions.get(geoPositions.size()-1), Color.BLUE);

                g.dispose();
            }
        };
        mapKit.getMainMap().setOverlayPainter(lineOverlay);
    }
    
    /**
     * Draws a red line which connects all GeoPosition of the track.
     * 
     * @param g the Graphics2D context
     * @param geoPositions list of GeoPosition objects of this track
     */
    private void drawTrackLine(Graphics2D g, List<GeoPosition> geoPositions) {
        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(2));

        int lastX = -1;
        int lastY = -1;
        
        for (GeoPosition geoPosition : geoPositions) {
            Point2D pt = convertGeoPosToPixelPos(geoPosition);
            if (lastX != -1 && lastY != -1) {
                g.drawLine(lastX, lastY, (int) pt.getX(), (int) pt.getY());
            }
            lastX = (int) pt.getX();
            lastY = (int) pt.getY();
        }
    }

    /**
     * Draws a waypoint circle at the specified GeoPosition.
     * 
     * @param g the Graphics2D context
     * @param geoPosition position of the waypoint
     * @param color the color of the circle
     */
    private void drawWaypoint(Graphics2D g, GeoPosition geoPosition, Color color) {
        g.setColor(color);
        g.setStroke(new BasicStroke(3));

        Point2D pt = convertGeoPosToPixelPos(geoPosition);
        g.draw(new Ellipse2D.Double(pt.getX() - 5, pt.getY() - 5, 10, 10));
    }
    
    private List<GeoPosition> createGeoPositionList(EVExercise exercise) {
        ArrayList<GeoPosition> geoPositions = new ArrayList<GeoPosition>();
        
        for (ExerciseSample exerciseSample : exercise.getSampleList()) {
            Position pos = exerciseSample.getPosition();
            if (pos != null) {
                geoPositions.add(new GeoPosition(pos.getLatitude(), pos.getLongitude()));
            }
        }
        return geoPositions;
    }

    private Point2D convertGeoPosToPixelPos(GeoPosition geoPosition) {
        return mapKit.getMainMap().getTileFactory().geoToPixel(geoPosition, mapKit.getMainMap().getZoom());
    }    
}
