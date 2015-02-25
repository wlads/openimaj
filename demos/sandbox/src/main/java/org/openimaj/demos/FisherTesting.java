package org.openimaj.demos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openimaj.feature.local.list.MemoryLocalFeatureList;
import org.openimaj.image.feature.local.aggregate.FisherVector;
import org.openimaj.image.feature.local.keypoints.FloatKeypoint;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.openimaj.math.matrix.algorithm.pca.ThinSvdPrincipalComponentAnalysis;
import org.openimaj.math.statistics.distribution.MixtureOfGaussians;
import org.openimaj.ml.gmm.GaussianMixtureModelEM;
import org.openimaj.ml.gmm.GaussianMixtureModelEM.CovarianceType;
import org.openimaj.util.array.ArrayUtils;

import Jama.Matrix;

public class FisherTesting {
	public static void main(String[] args) throws IOException {
		final List<MemoryLocalFeatureList<FloatKeypoint>> data = new ArrayList<MemoryLocalFeatureList<FloatKeypoint>>();
		final List<FloatKeypoint> allKeys = new ArrayList<FloatKeypoint>();

		for (int i = 0; i < 100; i++) {
			final MemoryLocalFeatureList<FloatKeypoint> tmp = FloatKeypoint.convert(MemoryLocalFeatureList.read(
					new File(String.format("/Users/jsh2/Data/ukbench/sift/ukbench%05d.jpg", i)), Keypoint.class));
			data.add(tmp);
			allKeys.addAll(tmp);
		}

		Collections.shuffle(allKeys);

		final double[][] sample128 = new double[1000][];
		for (int i = 0; i < sample128.length; i++) {
			sample128[i] = ArrayUtils.convertToDouble(allKeys.get(i).vector);
		}

		System.out.println("Performing PCA " + sample128.length);
		final ThinSvdPrincipalComponentAnalysis pca = new ThinSvdPrincipalComponentAnalysis(64);
		pca.learnBasis(sample128);
		final double[][] sample64 = pca.project(new Matrix(sample128)).getArray();

		System.out.println("Projecting features");
		for (final MemoryLocalFeatureList<FloatKeypoint> kpl : data) {
			for (final FloatKeypoint kp : kpl) {
				kp.vector = ArrayUtils.convertToFloat(pca.project(ArrayUtils.convertToDouble(kp.vector)));
			}
		}

		System.out.println("Learning GMM " + sample64.length);
		final GaussianMixtureModelEM gmmem = new GaussianMixtureModelEM(512, CovarianceType.Diagonal);
		final MixtureOfGaussians gmm = gmmem.estimate(sample64);

		final double[][] v1 = gmm.logProbability(new double[][] { sample64[0] });
		final double[][] v2 = MixtureOfGaussians.logProbability(new double[][] { sample64[0] }, gmm.gaussians);

		System.out.println("Done");

		// for (int i = 0; i < 512; i++) {
		// System.out.println(gmm.gaussians[i].getMean().get(0, 0) + "," +
		// gmm.gaussians[i].getMean().get(0, 1));
		// }

		final FisherVector<float[]> fisher = new FisherVector<float[]>(gmm, true, true);

		final List<FloatKeypoint> kpl = allKeys.subList(0, 26000);
		final long t1 = System.currentTimeMillis();
		fisher.aggregate(kpl).asDoubleVector();
		final long t2 = System.currentTimeMillis();
		System.out.println(t2 - t1);

		// int i = 0;
		// final double[][] fvs = new double[5][];
		// for (final MemoryLocalFeatureList<FloatKeypoint> kpl : data) {
		// final long t1 = System.currentTimeMillis();
		// fvs[i++] = fisher.aggregate(kpl).asDoubleVector();
		// final long t2 = System.currentTimeMillis();
		// System.out.println(t2 - t1);
		//
		// if (i == 5)
		// break;
		// }
		//
		// final ThinSvdPrincipalComponentAnalysis pca2 = new
		// ThinSvdPrincipalComponentAnalysis(128);
		// pca2.learnBasis(fvs);
	}
}
