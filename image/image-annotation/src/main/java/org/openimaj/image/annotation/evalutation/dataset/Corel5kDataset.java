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
package org.openimaj.image.annotation.evalutation.dataset;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openimaj.experiment.dataset.ListDataset;
import org.openimaj.feature.FeatureVector;
import org.openimaj.image.MBFImage;
import org.openimaj.image.annotation.AutoAnnotation;
import org.openimaj.image.annotation.BatchAnnotator;
import org.openimaj.image.annotation.ImageFeatureAnnotationProvider;
import org.openimaj.image.annotation.ImageFeatureProvider;
import org.openimaj.image.annotation.xform.DenseLinearTransformAnnotator;
import org.openimaj.image.pixel.statistics.HistogramModel;

public class Corel5kDataset extends ListDataset<CorelAnnotatedImage> {
	public static abstract class AnnotatorConnector implements BatchAnnotator<FeatureVector> {
		BatchAnnotator<FeatureVector> annotator;

		@Override
		public List<AutoAnnotation> annotate(ImageFeatureProvider<FeatureVector> provider) {
			return annotator.annotate(provider);
		}

		@Override
		public void train(List<ImageFeatureAnnotationProvider<FeatureVector>> data) {
			annotator.train(data);
		}
		
		public List<AutoAnnotation> annotate(final MBFImage image) {
			return annotator.annotate(new ImageFeatureProvider<FeatureVector>() {
				@Override
				public MBFImage getImage() {
					return image;
				}

				@Override
				public FeatureVector getFeature() {
					return createFeature(image);
				}
			});
		}
		
		protected abstract FeatureVector createFeature(MBFImage image);

		public void train(ListDataset<CorelAnnotatedImage> data) {
			List<ImageFeatureAnnotationProvider<FeatureVector>> cdata = new ArrayList<ImageFeatureAnnotationProvider<FeatureVector>>();
			
			for (final CorelAnnotatedImage img : data) {
				cdata.add(new ImageFeatureAnnotationProvider<FeatureVector>() {
					@Override
					public MBFImage getImage() {
						try {
							return img.getImage();
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}

					@Override
					public FeatureVector getFeature() {
						try {
							return createFeature(img.getImage());
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}

					@Override
					public List<String> getAnnotations() {
						return img.annotations;
					}
				});
			}
			
			annotator.train(cdata);
		}
	}
	
	File baseDir = new File("/Users/jsh2/Data/corel-5k");
	File imageDir = new File(baseDir, "images");
	File metaDir = new File(baseDir, "metadata");
	
	public Corel5kDataset() throws IOException {
		for (File f : imageDir.listFiles()) {
			if (f.getName().endsWith(".jpeg")) {
				String id = f.getName().replace(".jpeg", "");
				
				addItem(new CorelAnnotatedImage(id, f, new File(metaDir, id+"_1.txt")));
			}
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void main(String[] args) throws IOException {
		Corel5kDataset alldata = new Corel5kDataset();
		
		StandardCorel5kSplit split = new StandardCorel5kSplit();
		split.split(alldata);
		
		ListDataset<CorelAnnotatedImage> training = split.getTrainingDataset();

		AnnotatorConnector ac = new AnnotatorConnector() {
			@Override
			protected FeatureVector createFeature(MBFImage image) {
				HistogramModel hm = new HistogramModel(4,4,4);
				hm.estimateModel(image);
				return hm.getFeatureVector();
			}
		};
		
		ac.annotator = (BatchAnnotator)new DenseLinearTransformAnnotator<FeatureVector>();
		ac.train(training);
		
		for (CorelAnnotatedImage img : split.getTestDataset()) {
			System.out.println(ac.annotate(img.getImage()));
		}
	}
}