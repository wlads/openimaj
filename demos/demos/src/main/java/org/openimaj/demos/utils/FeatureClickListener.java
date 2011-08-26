/**
 * Copyright (c) 2011, The University of Southampton and the individual contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * 	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 *
 *   *	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 *
 *   *	Neither the name of the University of Southampton nor the names of its
 * 	contributors may be used to endorse or promote products derived from this
 * 	software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openimaj.demos.utils;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.Image;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.feature.local.engine.InterestPointImageExtractorProperties;
import org.openimaj.image.feature.local.interest.InterestPointData;
import org.openimaj.image.feature.local.interest.InterestPointVisualiser;
import org.openimaj.image.feature.local.keypoints.InterestPointKeypoint;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.processing.resize.ResizeProcessor;
import org.openimaj.image.processor.SinglebandImageProcessor;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Circle;
import org.openimaj.math.geometry.shape.Ellipse;
import org.openimaj.math.geometry.shape.Rectangle;

public class FeatureClickListener<S,T extends Image<S,T> & SinglebandImageProcessor.Processable<Float,FImage,T> > implements MouseListener {

	private List<InterestPointData> points = null;
	private T image;
	private JFrame frame = null;
	private ResizeProcessor r = new ResizeProcessor(100,100);
	
	private boolean areaSelected = false;
	private Point2dImpl pressClick;
	private Point2dImpl releaseClick;
	private JFrame displayFrame;
	private Rectangle selectedArea;
	
	public FeatureClickListener(){
		DisplayUtilities.createNamedWindow("blurwarp", "Warped Blurred patch",true);
		DisplayUtilities.createNamedWindow("blurnorm", "Normal Blurred patch",true);
		DisplayUtilities.createNamedWindow("warp", "Warpped patch",true);
		DisplayUtilities.createNamedWindow("norm", "Normal patch",true);
	}
	
	@Override
	public synchronized void mouseClicked(MouseEvent e) {
		if(this.points == null) return;
		if(this.areaSelected && e.isControlDown()){
			this.areaSelected = false;
			this.selectArea(this.image.getBounds());
		}
		double smallestScale = Double.MAX_VALUE;
		double smallestDistance = Double.MAX_VALUE;
		Ellipse foundShape = null;
		InterestPointData foundPoint = null;
		Point2dImpl clickPoint = new Point2dImpl(e.getPoint().x,e.getPoint().y);
		for(InterestPointData point : points){
			if(this.selectedArea.isInside(point)){
				Ellipse ellipse = point.getEllipse();
				if(ellipse.isInside(clickPoint)){
//					double currentScale = point.scale;
//					if(currentScale < smallestScale){
//						foundShape = ellipse;
//						foundPoint = point;
//						smallestScale = currentScale;
//					}
					double distance = Math.sqrt(Math.pow(clickPoint.x - point.x,2) + Math.pow(clickPoint.y - point.y,2));
					if(distance < smallestDistance){
						foundShape = ellipse;
						foundPoint = point;
						smallestDistance = distance;
					}
				}
			}
		}
		if(foundShape!=null){
//			PolygonExtractionProcessor<S, T> ext = new PolygonExtractionProcessor<S,T>(foundShape, image.newInstance(1, 1).getPixel(0,0));
			FGaussianConvolve blur = new FGaussianConvolve (foundPoint.scale);
			InterestPointImageExtractorProperties<S, T> extractWarp = new InterestPointImageExtractorProperties<S,T>(image,foundPoint,true);
			InterestPointImageExtractorProperties<S, T> extractNorm = new InterestPointImageExtractorProperties<S,T>(image,foundPoint,false);
			
			int centerX = extractNorm.halfWindowSize;
			int centerY = extractNorm.halfWindowSize;
			// Clone ellipses
			Ellipse warppedEllipse = new Ellipse(centerX,centerY,foundPoint.scale,foundPoint.scale,0);
			Ellipse normalEllipse = new Ellipse(centerX,centerY,foundShape.getMajor(),foundShape.getMinor(),foundShape.getRotation());
			
			T extractedWarpBlurred = extractWarp.image.process(blur);
			T extractedNormBlurred = extractNorm.image.process(blur);
			T extractedWarp = extractWarp.image.clone();
			T extractedNorm = extractNorm.image.clone();
			extractedWarpBlurred.drawShape(warppedEllipse, (S) RGBColour.RED);
			extractedNormBlurred.drawShape(normalEllipse, (S) RGBColour.RED);
			
			extractedWarp.drawShape(warppedEllipse, (S) RGBColour.RED);
			extractedNorm.drawShape(normalEllipse, (S) RGBColour.RED);
			
			DisplayUtilities.displayName(extractedWarpBlurred, "blurwarp");
			DisplayUtilities.displayName(extractedNormBlurred, "blurnorm");
			DisplayUtilities.displayName(extractedWarp, "warp");
			DisplayUtilities.displayName(extractedNorm, "norm");
			
			DisplayUtilities.positionNamed("blurwarp",image.getWidth(),0);
			DisplayUtilities.positionNamed("blurnorm",image.getWidth()+DisplayUtilities.createNamedWindow("blurwarp").getWidth(),0);
			DisplayUtilities.positionNamed("warp",image.getWidth(),DisplayUtilities.createNamedWindow("blurwarp").getHeight());
			DisplayUtilities.positionNamed("norm",image.getWidth()+DisplayUtilities.createNamedWindow("blurwarp").getWidth(),DisplayUtilities.createNamedWindow("blurwarp").getHeight());
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if(e.isAltDown()){
			pressClick = new Point2dImpl(e.getX(),e.getY());
		}
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if(e.isAltDown()){
			releaseClick = new Point2dImpl(e.getX(),e.getY());
			this.areaSelected = true;
			float px = pressClick.getX();
			float py = pressClick.getY();
			float rx = releaseClick.getX();
			float ry = releaseClick.getY();
			selectArea(new Rectangle(Math.min(px, rx),Math.min(py, ry),Math.abs(px-rx),Math.abs(py-ry)));
		}
		
	}

	private void selectArea(Rectangle rectangle) {
		this.selectedArea = rectangle;
		List<InterestPointData> pointsInsideRect = new ArrayList<InterestPointData>();
		for(InterestPointData  point: points){
			if(rectangle.isInside(point)){
				pointsInsideRect.add(point);
			}
		}
		InterestPointVisualiser<Float[], MBFImage> visualiser = InterestPointVisualiser.visualiseInterestPoints((MBFImage)image, pointsInsideRect);
		MBFImage out = visualiser.drawPatches(RGBColour.RED, RGBColour.GREEN);
		DisplayUtilities.display(out,this.displayFrame);
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}


	public List<? extends InterestPointData> getPoints() {
		return points;
	}

	public synchronized void setImage(List<? extends InterestPointData> points,T image) {
		this.image = image;
		this.selectedArea = image.getBounds();
		this.points = new ArrayList<InterestPointData>();
		for(InterestPointData x : points){
			this.points.add(x);
		}
	}

	public T getImage() {
		return image;
	}

	public void setImage(LocalFeatureList<? extends InterestPointKeypoint<?>> kps,T image) {
		this.image = image;
		this.selectedArea = image.getBounds();
		this.points = new ArrayList<InterestPointData>();
		for(InterestPointKeypoint<?> x : kps){
			this.points.add(x.location);
		}
		
	}

	public void setDisplayFrame(JFrame f) {
		this.displayFrame = f;
	}

}
