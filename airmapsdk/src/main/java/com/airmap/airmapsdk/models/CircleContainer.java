package com.airmap.airmapsdk.models;

import android.content.Context;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;

import com.airmap.airmapsdk.Analytics;
import com.airmap.airmapsdk.R;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.Source;

import java.util.ArrayList;
import java.util.List;

public class CircleContainer extends Container {

    private LatLng center;
    private double radius;
    private List<LatLng> points;

    public CircleContainer(Context context, MapboxMap map) {
        super(context, map);
    }

    public void drawOnMap(LatLng center, double radius) {
        this.center = center;
        this.radius = radius;
        this.points = calculateCirclePoints(center, radius);

        List<Point> positions = latLngsToPositions(points);
        List<List<Point>> coordinates = new ArrayList<>();
        coordinates.add(positions);

        List<Point> lineString = new ArrayList<>(positions);
        lineString.add(positions.get(0));

        // if polygon layer doesn't exist, create and add to map
        if (map.getStyle().getLayer(POINT_LAYER) == null) {
            Source pointSource = new GeoJsonSource(POINT_SOURCE, Feature.fromGeometry(Point.fromLngLat(center.getLongitude(), center.getLatitude())));
            map.getStyle().addSource(pointSource);
            Layer pointLayer = new SymbolLayer(POINT_LAYER, POINT_SOURCE)
                .withProperties(PropertyFactory.iconImage(CORNER_IMAGE));
            map.getStyle().addLayer(pointLayer);

            Source polygonSource = new GeoJsonSource(POLYGON_SOURCE, Feature.fromGeometry(Polygon.fromLngLats(coordinates)));
            map.getStyle().addSource(polygonSource);
            FillLayer polygonLayer = new FillLayer(POLYGON_LAYER, POLYGON_SOURCE)
                    .withProperties(PropertyFactory.fillColor(ContextCompat.getColor(context, R.color.colorAccent)), PropertyFactory.fillOpacity(0.5f));
            map.getStyle().addLayerBelow(polygonLayer, POINT_LAYER);

            Source polylineSource = new GeoJsonSource(POLYLINE_SOURCE, Feature.fromGeometry(LineString.fromLngLats(lineString)));
            map.getStyle().addSource(polylineSource);
            Layer polylineLayer = new LineLayer(POLYLINE_LAYER, POLYLINE_SOURCE)
                    .withProperties(PropertyFactory.lineColor(ContextCompat.getColor(context, R.color.colorPrimary)), PropertyFactory.lineOpacity(0.9f));
            map.getStyle().addLayerAbove(polylineLayer, POLYGON_LAYER);

        // otherwise, update source
        } else {
            GeoJsonSource polygonSource = map.getStyle().getSourceAs(POLYGON_SOURCE);
            polygonSource.setGeoJson(Feature.fromGeometry(Polygon.fromLngLats(coordinates)));

            FillLayer polygonFill = map.getStyle().getLayerAs(Container.POLYGON_LAYER);
            polygonFill.setProperties(PropertyFactory.fillColor(ContextCompat.getColor(context, R.color.colorAccent)));

            GeoJsonSource polylineSource = map.getStyle().getSourceAs(POLYLINE_SOURCE);
            polylineSource.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(lineString)));
        }
    }

    public void move(LatLng center) {
        this.center = center;
        this.points = calculateCirclePoints(center, radius);

        List<Point> positions = latLngsToPositions(points);
        List<List<Point>> coordinates = new ArrayList<>();
        coordinates.add(positions);

        List<Point> lineString = new ArrayList<>(positions);
        lineString.add(positions.get(0));

        GeoJsonSource pointSource = map.getStyle().getSourceAs(POINT_SOURCE);
        pointSource.setGeoJson(Feature.fromGeometry(Point.fromLngLat(center.getLongitude(), center.getLatitude())));

        GeoJsonSource polygonSource = map.getStyle().getSourceAs(POLYGON_SOURCE);
        polygonSource.setGeoJson(Feature.fromGeometry(Polygon.fromLngLats(coordinates)));

        FillLayer polygonFill = map.getStyle().getLayerAs(Container.POLYGON_LAYER);
        polygonFill.setProperties(PropertyFactory.fillColor(ContextCompat.getColor(context, R.color.colorAccent)));

        GeoJsonSource polylineSource = map.getStyle().getSourceAs(POLYLINE_SOURCE);
        polylineSource.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(lineString)));
    }

    @Override
    public LatLngBounds getLatLngBoundsForZoom() {
        Analytics.logDebug("center", center.toString());
        Analytics.logDebug("points", TextUtils.join(" - ", points));
        return new LatLngBounds.Builder().includes(points).build();
    }

    @Override
    public void clear() {
        center = null;
        points = null;

        map.getStyle().removeLayer(POINT_LAYER);
        map.getStyle().removeSource(POINT_SOURCE);

        map.getStyle().removeLayer(POLYGON_LAYER);
        map.getStyle().removeSource(POLYGON_SOURCE);

        map.getStyle().removeLayer(POLYLINE_LAYER);
        map.getStyle().removeSource(POLYLINE_SOURCE);
    }

    public boolean isValid() {
        return center != null && points != null;
    }

    public void setCenter(LatLng center) {
        this.center = center;
    }

    public LatLng getCenter() {
        return center;
    }

    public double getRadius() {
        return radius;
    }

    public List<LatLng> getPoints() {
        return points;
    }

    public static ArrayList<LatLng> calculateCirclePoints(LatLng location, double radius) {
        int degreesBetweenPoints = 8;
        int numberOfPoints = (int) Math.floor(360 / degreesBetweenPoints);
        double distRadians = radius / 6371000.0; // earth radius in meters
        double centerLatRadians = location.getLatitude() * Math.PI / 180;
        double centerLonRadians = location.getLongitude() * Math.PI / 180;
        ArrayList<LatLng> polygons = new ArrayList<>(); //array to hold all the path
        for (int index = 0; index < numberOfPoints; index++) {
            double degrees = index * degreesBetweenPoints;
            double degreeRadians = degrees * Math.PI / 180;
            double pointLatRadians = Math.asin(Math.sin(centerLatRadians) * Math.cos(distRadians) + Math.cos(centerLatRadians) * Math.sin(distRadians) * Math.cos(degreeRadians));
            double pointLonRadians = centerLonRadians + Math.atan2(Math.sin(degreeRadians) * Math.sin(distRadians) * Math.cos(centerLatRadians),
                    Math.cos(distRadians) - Math.sin(centerLatRadians) * Math.sin(pointLatRadians));
            double pointLat = pointLatRadians * 180 / Math.PI;
            double pointLon = pointLonRadians * 180 / Math.PI;
            polygons.add(new LatLng(pointLat, pointLon));
        }
        return polygons;
    }
}
