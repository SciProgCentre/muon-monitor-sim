package ru.mipt.npm.muon.sim

import org.apache.commons.math3.analysis.MultivariateFunction
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction
import org.apache.commons.math3.analysis.polynomials.PolynomialsUtils
import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.exception.DimensionMismatchException
import org.apache.commons.math3.optim.InitialGuess
import org.apache.commons.math3.optim.MaxEval
import org.apache.commons.math3.optim.MaxIter
import org.apache.commons.math3.optim.PointValuePair
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer
import org.apache.commons.math3.util.Pair
import java.util.*

/**
 * Code partially taken from  https://sourceforge.net/u/mattclarkson/camino/ci/5a512e47f7bd4d1a69f400afe0318b6c0c260602/tree/numerics/SphericalHarmonics.java
 * Created by darksnake on 16-Oct-16.
 */
object SphericalHarmonics {

    /**
     * Computes the factor of the Legendre polynomial used to compute the
     * spherical harmonics.
     * @param l Spherical harmonic order
     * @param m Spherical harmonic index
     * @return The scaling factor.
     */
    private fun getFactor(l: Int, m: Int): Double {

        var factor = 1.0
        for (i in l - m + 1..l + m) {
            factor *= i.toDouble()
        }
        factor = (2 * l + 1).toDouble() / factor
        factor /= 4.0 * Math.PI
        factor = Math.sqrt(factor)

        return factor
    }

    /**
     * Legendre functions cache
     */
    private val legendreCache = HashMap<Pair<Int, Int>, UnivariateDifferentiableFunction>(30)

    /**
     * Calculate associated legendre functions. Use cache to store functions
     * @param l
     * @param m
     * @return
     */
    @JvmStatic fun associatedLegendre(l: Int, m: Int): UnivariateDifferentiableFunction {
        return legendreCache.computeIfAbsent(Pair(l, m)) { pair ->
            var legendre = PolynomialsUtils.createLegendrePolynomial(l)
            for (i in 0..m - 1) {
                legendre = legendre.polynomialDerivative()
            }
            val res = legendre
            val sup = PolynomialFunction(doubleArrayOf(1.0, 0.0, -1.0))//1-x^2
            object : UnivariateDifferentiableFunction {

                @Throws(DimensionMismatchException::class)
                override fun value(t: DerivativeStructure): DerivativeStructure {
                    return res.value(t).multiply(Math.pow(-1.0, m.toDouble())).multiply(sup.value(t).pow(m.toDouble() / 2.0))
                }

                override fun value(x: Double): Double {
                    return value(DerivativeStructure(1, 0, x)).value
                }
            }
        }
    }

    @JvmStatic fun plgndr(l: Int, m: Int, x: Double): Double {
        return associatedLegendre(l, m).value(x)
    }


    /**
     * NRC method for computing associated Legendre polynomial values.
     * @param l The order of the polynomial
     * @param m The index of the polynomial
     * @param x The argument.
     * @return the value of the polynomial at x.
     */
    @JvmStatic fun plgndrTest(l: Int, m: Int, x: Double): Double {
        if (m < 0 || m > l || Math.abs(x) > 1.0) {
            throw RuntimeException("Bad arguments in routine plgndr")
        }
        var pmm = 1.0
        if (m > 0) {
            val somx2 = Math.sqrt((1.0 - x) * (1.0 + x))
            var fact = 1.0
            for (i in (1..m)) {
                pmm *= -fact * somx2
                fact += 2.0
            }
        }
        if (l == m)
            return pmm
        else {
            var pmmp1 = x * (2 * m + 1).toDouble() * pmm
            if (l == m + 1)
                return pmmp1
            else {
                var pll = 0.0
                for (ll in (m + 2..l)) {
                    pll = (x * (2 * ll - 1).toDouble() * pmmp1 - (ll + m - 1) * pmm) / (ll - m)
                    pmm = pmmp1
                    pmmp1 = pll
                }
                return pll
            }
        }
    }

    /**
     * Computes the number of spherical harmonic functions up to a specified
     * order.
     * @param order The order up to which the number of functions is required.
     * @return The number of functions.
     */
    @JvmStatic fun funcsUpTo(order: Int): Int {
        return (order + 1) * (order + 1)
    }

    /**
     * Computes the number of even spherical harmonic functions up to a
     * specified order.
     * @param order The order up to which the number of functions is required.
     * @return The number of functions.
     */
    @JvmStatic fun evenFuncsUpTo(order: Int): Int {
        var res = order

        // If order is odd, change it to the even order just below.
        if (res % 2 == 1) {
            res--;
        }

        return (res + 1) * (res / 2 + 1)
    }

    /**
     * Computes spherical harmonic of order l, index m at colatitude theta and
     * longitude phi.
     * @param l     The spherical harmonic order.
     * @param m     The spherical harmonic index.
     * @param theta The angle of colatitude.
     * @param phi   The angle of longitude.
     * @return Y_lm(theta, phi)
     */
    @JvmStatic fun spherical(l: Int, m: Int, theta: Double, phi: Double): Complex {
        val mPos = Math.abs(m)

        //Normalisation factor for spherical harmonic function.
        val factor = getFactor(l, mPos)

        //Associated Legendre poly of cos(theta)
        val thetaPart = plgndr(l, mPos, Math.cos(theta))

        //exp(i.m.phi)
        val phiPart = Complex(Math.cos(mPos.toDouble() * phi),
                Math.sin(mPos.toDouble() * phi))

        //Combine them all to get the result.
        var result = phiPart.multiply(factor * thetaPart)

        //If m is negative take the conjugate of the positive
        //result, negated if m is odd.
        if (m < 0) {
            result = result.conjugate()
            if (mPos % 2 == 1) {
                result = result.negate()
            }
        }

        return result
    }

    /**
     * Spherical coefficient
     */
    data class Coef(val l: Int, val m: Int, val c: Complex);

    /**
     * Calculate sum of spherical function with given coefficients
     */
    @JvmStatic fun sphericalValue(theta: Double, phi: Double, coefs: Array<Coef>): Double {
        return coefs.sumByDouble { it -> spherical(it.l, it.m, theta, phi).multiply(it.c).real }
    }

    /**
     * Convert double array to spherical function coefficients
     */
    @JvmStatic fun arrayToCoefs(arr: DoubleArray): Array<Coef> {
        if ((arr.size % 2) != 0) {
            error("coefficients size must be even");
        }
        val res = ArrayList<Coef>();

        var l = 0;
        var m = 0;
        var counter = 0;
        while (counter < arr.size) {
            val re = arr[counter];
            val im = arr[counter + 1];
            res.add(Coef(l, m, Complex(re, im)))
            counter += 2;
            if (m == l) {
                l++;
                m = 0;
            } else{
                m++;
            }
        }

        return res.toTypedArray();
    }

    data class Point(val theta: Double, val phi: Double, val value: Double, val err: Double);

    /**
     * The value of chi2 for given data and set of coefficients
     */
    @JvmStatic fun chi2SphericalValue(points: Array<Point>, coefs: Array<Coef>): Double {
        return points.sumByDouble {
            Math.pow((it.value - sphericalValue(it.theta, it.phi, coefs)) / it.err, 2.0);
        }
    }

    /**
     * Calculate chi2 for given data set
     */
    @JvmStatic fun chi2SphericalFunction(points: Array<Point>): MultivariateFunction {
        return MultivariateFunction { pars -> chi2SphericalValue(points, arrayToCoefs(pars)) }
    }

    /**
     * Fit data using no-deriv procedure
     * @param points data points
     * @param l - maximum l:
     */
    @JvmStatic fun fit(points: Array<Point>, l: Number): PointValuePair {
        val dim = 2 * evenFuncsUpTo(l.toInt());
        return fit(points,DoubleArray(dim, { i -> 0.0 }), dim);
    }

    @JvmStatic fun fit(points: Array<Point>, start: DoubleArray, dim: Int): PointValuePair {
        val optimizer = SimplexOptimizer(0.01, 0.01);
        return optimizer.optimize(
                ObjectiveFunction(chi2SphericalFunction(points)),
                GoalType.MINIMIZE,
                NelderMeadSimplex(DoubleArray(dim, { i -> 100.0 })),
                InitialGuess(start),
                MaxEval(1e7.toInt()),
                MaxIter(1e5.toInt())
        )
    }


}

