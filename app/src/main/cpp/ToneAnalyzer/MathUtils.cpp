#include "MathUtils.h"
#include <math.h>
#include "matrix.h"
#include <stdlib.h>
#include <string.h>
#include <vector>
#include <iostream>
#include <algorithm>

#define PI 3.141592653589793116


template<typename T>
class Givens {
public:
    Givens() : m_oJ(2, 2), m_oQ(1, 1), m_oR(1, 1) {
    }

    /*
     Calculate the inverse of a matrix using the QR decomposition.
     
     param:
     A	matrix to inverse
     */
    const Matrix<T> Inverse(Matrix<T> &oMatrix) {
        if (oMatrix.cols() != oMatrix.rows()) {
            //throw domain_error( "matrix has to be square" );
        }
        Matrix<T> oIdentity = Matrix<T>::identity(oMatrix.rows());
        Decompose(oMatrix);
        return Solve(oIdentity);
    }

    /*
     Performs QR factorization using Givens rotations.
     */
    void Decompose(Matrix<T> &oMatrix) {
        int nRows = oMatrix.rows();
        int nCols = oMatrix.cols();


        if (nRows == nCols) {
            nCols--;
        } else if (nRows < nCols) {
            nCols = nRows - 1;
        }

        m_oQ = Matrix<T>::identity(nRows);
        m_oR = oMatrix;

        for (int j = 0; j < nCols; j++) {
            for (int i = j + 1; i < nRows; i++) {
                GivensRotation(m_oR(j, j), m_oR(i, j));
                PreMultiplyGivens(m_oR, j, i);
                PreMultiplyGivens(m_oQ, j, i);
            }
        }

        m_oQ = m_oQ.transpose();
    }

    /*
     Find the solution for a matrix.
     http://en.wikipedia.org/wiki/QR_decomposition#Using_for_solution_to_linear_inverse_problems
     */
    Matrix<T> Solve(Matrix<T> &oMatrix) {
        Matrix<T> oQtM(m_oQ.transpose() * oMatrix);
        int nCols = m_oR.cols();
        Matrix<T> oS(1, nCols);
        for (int i = nCols - 1; i >= 0; i--) {
            oS(0, i) = oQtM(i, 0);
            for (int j = i + 1; j < nCols; j++) {
                oS(0, i) -= oS(0, j) * m_oR(i, j);
            }
            oS(0, i) /= m_oR(i, i);
        }

        return oS;
    }

    const Matrix<T> &GetQ() {
        return m_oQ;
    }

    const Matrix<T> &GetR() {
        return m_oR;
    }

private:
    /*
     Givens rotation is a rotation in the plane spanned by two coordinates axes.
     http://en.wikipedia.org/wiki/Givens_rotation
     */
    void GivensRotation(T a, T b) {
        T t, s, c;
        if (b == 0) {
            c = (a >= 0) ? 1 : -1;
            s = 0;
        } else if (a == 0) {
            c = 0;
            s = (b >= 0) ? -1 : 1;
        } else if (fabs(b) > fabs(a)) {
            t = a / b;
            s = -1 / sqrt(1 + t * t);
            c = -s * t;
        } else {
            t = b / a;
            c = 1 / sqrt(1 + t * t);
            s = -c * t;
        }
        m_oJ(0, 0) = c;
        m_oJ(0, 1) = -s;
        m_oJ(1, 0) = s;
        m_oJ(1, 1) = c;
    }

    /*
     Get the premultiplication of a given matrix 
     by the Givens rotation.
     */
    void PreMultiplyGivens(Matrix<T> &oMatrix, int i, int j) {
        int nRowSize = oMatrix.cols();

        for (unsigned int nRow = 0; nRow < nRowSize; nRow++) {
            double nTemp = oMatrix(i, nRow) * m_oJ(0, 0) + oMatrix(j, nRow) * m_oJ(0, 1);
            oMatrix(j, nRow) = oMatrix(i, nRow) * m_oJ(1, 0) + oMatrix(j, nRow) * m_oJ(1, 1);
            oMatrix(i, nRow) = nTemp;
        }
    }

private:
    Matrix<T> m_oQ, m_oR, m_oJ;
};

//template <typename T>
int MathUtils::sgn(double val) {
    return (0 < val) - (val < 0);
}

void MathUtils::deriv(double *a, double *d, int n) {
    if (n < 2)
        return;

    d[0] = a[1] - a[0];
    d[n - 1] = a[n - 1] - a[n - 2];
    for (int j = 2; j <= n - 1; j++) {
        d[j - 1] = (a[j + 1 - 1] - a[j - 1 - 1]) / 2;
    }
}

double MathUtils::mean(double *arr, int start, int end) {
    if (start >= end)
        return 0;

    double sum = 0;
    for (int i = start; i < end; i++)
        sum += arr[i];
    return sum / (end - start);
}

double MathUtils::std(double *in, int n) {
    if (n == 1)
        return 0;

    double deviation;
    double sum2 = 0;
    double ave = mean(in, 0, n);

    for (int i = 0; i < n; i++) {
        sum2 += powf((in[i] - ave), 2);
    }
    deviation = sqrt(sum2 / (n - 1));
    return deviation;
}


double MathUtils::variance(double *in, int n) {
    double deviation;
    double sum2 = 0;
    double ave = mean(in, 0, n);

    for (int i = 0; i <= n; i++) {
        sum2 += powf((in[i] - ave), 2);
    }
    deviation = sum2 / (n - 1);
    return deviation;
}

/**
	* Smoothing function fastsmooth(Y,w,type,ends) smooths vector Y with smooth
	* of width w. Version 2.0, May 2008.
	*  <p>
	* The argument "type" determines the smooth type:
	*   If type=1, rectangular (sliding-average or boxcar)
	*   If type=2, triangular (2 passes of sliding-average)
	*   If type=3, pseudo-Gaussian (3 passes of sliding-average)
	* The argument "ends" controls how the "ends" of the signal
	* (the first w/2 points and the last w/2 points) are handled.
	*   If ends=0, the ends are zero.  (In this mode the elapsed
	*     time is independent of the smooth width). The fastest.
	*   If ends=1, the ends are smoothed with progressively
	*     smaller smooths the closer to the end. (In this mode the
	*     elapsed time increases with increasing smooth widths).
	* fastsmooth(Y,w,type) smooths with ends=0.
	* fastsmooth(Y,w) smooths with type=1 and ends=0.
	* Example:
	* fastsmooth([1 1 1 10 10 10 1 1 1 1],3)= [0 1 4 7 10 7 4 1 1 0]
	* fastsmooth([1 1 1 10 10 10 1 1 1 1],3,1,1)= [1 1 4 7 10 7 4 1 1 1]
	*  T. C. O'Haver, 2008.
	*/
void MathUtils::fastsmooth(double *Y, double *out, double w, int type, int ends, int L) {
    switch (type) {
        case 1:
            sa(Y, out, w, ends, L);
            break;
        case 2:
            sa(Y, out, w, ends, L);
            memcpy(Y, out, sizeof(double) * L);
            sa(Y, out, w, ends, L);
            break;
        case 3:
            sa(Y, out, w, ends, L);
            memcpy(Y, out, sizeof(double) * L);
            sa(Y, out, w, ends, L);
            memcpy(Y, out, sizeof(double) * L);
            sa(Y, out, w, ends, L);
            break;
    }

}

void MathUtils::sa(double *Y, double *s, double smoothwidth, int ends, int L) {
    int w = (int) round(smoothwidth);
    double SumPoints = 0;
    for (int i = 0; i < w; i++) {
        SumPoints += Y[i];
    }

    int halfw = (int) round(w / 2.);

    for (int k = 1; k <= L - w; k++) {
        s[k + halfw - 1 - 1] = SumPoints / w;
        SumPoints = SumPoints - Y[k - 1];
        SumPoints = SumPoints + Y[k + w - 1];
    }
    for (int i = L - w + 1; i <= L; i++) {
        s[L - w + halfw - 1] += Y[i - 1];
    }
    s[L - w + halfw - 1] /= w;

    if (ends == 1) {
        double startpoint = (smoothwidth + 1) / 2;
        s[0] = (Y[0] + Y[1]) / 2;
        for (int k = 2; k <= startpoint; k++) {
            s[k - 1] = mean(Y, 0, 2 * k - 1);
            s[L - k + 1 - 1] = mean(Y, L - 2 * k + 2 - 1, L);
        }
        s[L - 1] = (Y[L - 1] + Y[L - 2]) / 2;
    }
}

double
MathUtils::zeroGoal(float *signal, int size, double frequency, Complex *grid, Complex *gridSignal) {
    Complex fprime = 0;
    Complex f = 0;
    for (int i = 0; i < size; i++) {
        Complex temp = exp(grid[i] * frequency);
        fprime = fprime + temp * gridSignal[i];
        f = f + temp * (double) signal[i];
    }
    double result = -1 * (f.real() * fprime.real() + f.imag() * fprime.imag());

    return result;
}


double MathUtils::optimalGoal(float *signal, int size, double frequency, Complex *grid) {
    Complex f = 0;
    for (int i = 0; i < size; i++) {
        Complex temp = exp(grid[i] * frequency);
        f = f + temp * (double) signal[i];
    }
    double result = -1 * (abs(f));

    return result;
}

double MathUtils::optimalGoal(float *signal, int size, double frequency, double *grid) {
    double thetaReal[size];
    double thetaImage[size];
    for (int i = 0; i < size; i++) {
        thetaReal[i] = cosf(frequency * grid[i]);
        thetaImage[i] = sinf(frequency * grid[i]);
    }
    double x = multiplyVectors(thetaReal, signal, size);
    double y = multiplyVectors(thetaImage, signal, size);
    double result = -1 * (sqrtf((x * x) + (y * y)));
    return result;
}

double MathUtils::multiplyVectors(double *a, float *b, int size) {
    if (size == 0)return 0;
    double result = 0;
    for (int i = 0; i < size; i++) {
        result += a[i] * b[i];
    }
    return result;
}

double MathUtils::fmin(double a, double b, double tol, int maxValFun, float *signal, int size,
                       Complex *grid, Complex *gridSignal, int method) {
    int funEvalCount = 0;
    int iterCount = 0;

    double c, d, e, eps, xm, p, q, r, tol1, t2, u, v, w, fu, fv, fw, fx, x, tol3;
    c = .5 * (3.0 - sqrtf(5.0));
    d = 0.0;

    // 1.1102e-16 is machine precision
    eps = 1.2e-16;
    tol1 = eps + 1.0;
    eps = sqrtf(eps);
    v = a + c * (b - a);
    w = v;
    x = v;
    e = 0.0;

    if (method == 0)
        fx = optimalGoal(signal, size, x, grid);
    else
        fx = zeroGoal(signal, size, x, grid, gridSignal);
    funEvalCount++;

    fv = fx;
    fw = fx;
    tol3 = tol / 3.0;
    xm = .5 * (a + b);
    tol1 = eps * fabs(x) + tol3;
    t2 = 2.0 * tol1;

    // main loop

    while (fabs(x - xm) > (t2 - .5 * (b - a))) {
        p = q = r = 0.0;
        if (fabs(e) > tol1) {
            // fit the parabola
            r = (x - w) * (fx - fv);
            q = (x - v) * (fx - fw);
            p = (x - v) * q - (x - w) * r;
            q = 2.0 * (q - r);
            if (q > 0.0) {
                p = -p;
            } else {
                q = -q;
            }
            r = e;
            e = d;
        }
        if ((fabs(p) < fabs(.5 * q * r)) &&
            (p > q * (a - x)) &&
            (p < q * (b - x))) {
            // a parabolic interpolation step
            d = p / q;
            u = x + d;
            // f must not be evaluated too close to a or b
            if (((u - a) < t2) || ((b - u) < t2)) {
                d = tol1;
                if (x >= xm) d = -d;
            }
        } else {
            // a golden-section step
            if (x < xm) {
                e = b - x;
            } else {
                e = a - x;
            }
            d = c * e;
        }
        // f must not be evaluated too close to x
        if (fabs(d) >= tol1) {
            u = x + d;
        } else {
            if (d > 0.0) {
                u = x + tol1;
            } else {
                u = x - tol1;
            }
        }
        if (method == 0)
            fu = optimalGoal(signal, size, u, grid);
        else
            fu = zeroGoal(signal, size, u, grid, gridSignal);

        funEvalCount++;
        iterCount++;

        // Update a, b, v, w, and x
        if (fx <= fu) {
            if (u < x) {
                a = u;
            } else {
                b = u;
            }
        }
        if (fu <= fx) {
            if (u < x) {
                b = x;
            } else {
                a = x;
            }
            v = w;
            fv = fw;
            w = x;
            fw = fx;
            x = u;
            fx = fu;
            xm = .5 * (a + b);
            tol1 = eps * fabs(x) + tol3;
            t2 = 2.0 * tol1;
        } else {
            if ((fu <= fw) || (w == x)) {
                v = w;
                fv = fw;
                w = u;
                fw = fu;
                xm = .5 * (a + b);
                tol1 = eps * fabs(x) + tol3;
                t2 = 2.0 * tol1;
            } else if ((fu > fv) && (v != x) && (v != w)) {
                xm = .5 * (a + b);
                tol1 = eps * fabs(x) + tol3;
                t2 = 2.0 * tol1;
            } else {
                v = u;
                fv = fu;
                xm = .5 * (a + b);
                tol1 = eps * fabs(x) + tol3;
                t2 = 2.0 * tol1;
            }
        }
        if (funEvalCount >= maxValFun || iterCount >= 500) {
            break;
        }
    }
    return x;
}

double MathUtils::fmin(double a, double b, double tol, int maxValFun, float *signal, int size,
                       double *grid, int method) {
    int funEvalCount = 0;
    int iterCount = 0;

    double c, d, e, eps, xm, p, q, r, tol1, t2, u, v, w, fu, fv, fw, fx, x, tol3;
    c = .5 * (3.0 - sqrtf(5.0));
    d = 0.0;

    // 1.1102e-16 is machine precision
    eps = 1.2e-16;
    tol1 = eps + 1.0;
    eps = sqrtf(eps);
    v = a + c * (b - a);
    w = v;
    x = v;
    e = 0.0;

    fx = optimalGoal(signal, size, x, grid);
    funEvalCount++;

    fv = fx;
    fw = fx;
    tol3 = tol / 3.0;
    xm = .5 * (a + b);
    tol1 = eps * fabs(x) + tol3;
    t2 = 2.0 * tol1;

    // main loop

    while (fabs(x - xm) > (t2 - .5 * (b - a))) {
        p = q = r = 0.0;
        if (fabs(e) > tol1) {
            // fit the parabola
            r = (x - w) * (fx - fv);
            q = (x - v) * (fx - fw);
            p = (x - v) * q - (x - w) * r;
            q = 2.0 * (q - r);
            if (q > 0.0) {
                p = -p;
            } else {
                q = -q;
            }
            r = e;
            e = d;
        }
        if ((fabs(p) < fabs(.5 * q * r)) &&
            (p > q * (a - x)) &&
            (p < q * (b - x))) {
            // a parabolic interpolation step
            d = p / q;
            u = x + d;
            // f must not be evaluated too close to a or b
            if (((u - a) < t2) || ((b - u) < t2)) {
                d = tol1;
                if (x >= xm) d = -d;
            }
        } else {
            // a golden-section step
            if (x < xm) {
                e = b - x;
            } else {
                e = a - x;
            }
            d = c * e;
        }
        // f must not be evaluated too close to x
        if (fabs(d) >= tol1) {
            u = x + d;
        } else {
            if (d > 0.0) {
                u = x + tol1;
            } else {
                u = x - tol1;
            }
        }

        fu = optimalGoal(signal, size, u, grid);

        funEvalCount++;
        iterCount++;

        // Update a, b, v, w, and x
        if (fx <= fu) {
            if (u < x) {
                a = u;
            } else {
                b = u;
            }
        }
        if (fu <= fx) {
            if (u < x) {
                b = x;
            } else {
                a = x;
            }
            v = w;
            fv = fw;
            w = x;
            fw = fx;
            x = u;
            fx = fu;
            xm = .5 * (a + b);
            tol1 = eps * fabs(x) + tol3;
            t2 = 2.0 * tol1;
        } else {
            if ((fu <= fw) || (w == x)) {
                v = w;
                fv = fw;
                w = u;
                fw = fu;
                xm = .5 * (a + b);
                tol1 = eps * fabs(x) + tol3;
                t2 = 2.0 * tol1;
            } else if ((fu > fv) && (v != x) && (v != w)) {
                xm = .5 * (a + b);
                tol1 = eps * fabs(x) + tol3;
                t2 = 2.0 * tol1;
            } else {
                v = u;
                fv = fu;
                xm = .5 * (a + b);
                tol1 = eps * fabs(x) + tol3;
                t2 = 2.0 * tol1;
            }
        }
        if (funEvalCount >= maxValFun || iterCount >= 500) {
            break;
        }
    }
    return x;
}

/**
 * fit:
 * returns the coefficients of the polynomial which best approximates f
 * (in the mean square error sense) for the given data.
 *  y = f(x).
 *  y' = a[0]*x^(order) + a[1]*x^(order-1) + ... + a[order-1]*x + a[order]
 *
 *  @param  ndata   number of data;
 *  @param  x       values of the indipendent variable
 *  @param  y       values of the dipendent variable (data)
 *  @param  order   the order of the polynomial
 *
 *  @return array (of order+1 double) of the coefficients of the polynomial
 */
void MathUtils::polyFit(double *x, int nCount, double *y, int nDegree, double *a) {
    nDegree++;

    //size_t nCount =  oX.size();
    Matrix<double> oXMatrix(nCount, nDegree);
    Matrix<double> oYMatrix(nCount, 1);

    // copy y matrix
    for (size_t i = 0; i < nCount; i++) {
        oYMatrix(i, 0) = y[i];
    }

    // create the X matrix
    for (size_t nRow = 0; nRow < nCount; nRow++) {
        double nVal = 1.0f;
        for (int nCol = 0; nCol < nDegree; nCol++) {
            oXMatrix(nRow, nCol) = nVal;
            nVal *= x[nRow];
        }
    }

    // transpose X matrix
    Matrix<double> oXtMatrix(oXMatrix.transpose());
    // multiply transposed X matrix with X matrix
    Matrix<double> oXtXMatrix(oXtMatrix * oXMatrix);
    // multiply transposed X matrix with Y matrix
    Matrix<double> oXtYMatrix(oXtMatrix * oYMatrix);

    Givens<double> oGivens;
    oGivens.Decompose(oXtXMatrix);
    Matrix<double> oCoeff = oGivens.Solve(oXtYMatrix);
    // copy the result to coeff
    std::vector<double> v = oCoeff.data();
    for (int i = 0; i < nDegree; i++) {
        a[nDegree - 1 - i] = v[i];
    }
}

void MathUtils::polyVal(double *p, int pLen, double *x, int xLen, double *res) {
    for (int i = 0; i < xLen; i++) {
        double sum = 0;
        double cur = x[i];
        for (int j = 0; j < pLen; j++) {
            int pow = pLen - j - 1;
            sum += p[j] * (powf(cur, pow));
        }
        res[i] = sum;
    }
}

void MathUtils::determineCholeskyMatrix(int order, double in[87][87], double out[87][87]) {
    int i = order;
    for (int k = 0; k < i; ++k) {
        for (int j = 0; j < i; ++j) {
            double s = 0.;

            double d2 = out[j][j];
            if (out[j][j] == 0)
                out[k][j] = 0;
            else {
                d2 = 1.0 / d2;
                for (int p = 0; p < j; ++p) {
                    s += out[k][p] * out[j][p] * d2;
                }

                out[k][j] = (in[k][j] * d2 - s);
            }
        }
        double s = 0.;
        for (int p = 0; p < k; ++p) {
            s += out[k][p] * out[k][p];
        }
        if (in[k][k] - s < 0)
            out[k][k] = 0;
        else
            out[k][k] = sqrtf(in[k][k] - s);
    }
}

void MathUtils::solveCholeskyMatrix(int m, double in[87][87], double *x, double *out) {
    int j = m;
    double y[j];
    for (int i = 0; i < j; ++i) {

        double s = 0.;
        for (int k = 0; k < i; ++k) {
            s += in[i][k] * y[k];
        }
        if (in[i * j + i] == 0)
            y[i] = 0;
        else
            y[i] = (x[i] - s) / in[i][i];
    }
    for (int i = j - 1; i >= 0; --i) {
        double s = 0.;
        for (int k = i + 1; k < j; ++k) {
            s += in[k][i] * out[k];
        }
        if (in[i][i] == 0)
            out[i] = 0;
        else
            out[i] = (y[i] - s) / in[i][i];
    }
}


double MathUtils::fminsearchFunction(double *bxfit, int length, int *bx, double *by, int bxlen,
                                     double *inharmon1, double *inharmon2, double *ub, double *lb) {
    double bxfitx[99];
    memcpy(bxfitx, bxfit, sizeof(double) * length);
    for (int i = 0; i < length; i++) {
        bxfitx[i] = (sin(bxfitx[i]) + 1) / 2;
        bxfitx[i] = bxfitx[i] * (ub[i] - lb[i]) + lb[i];
        //% just in case of any doubleing point problems
        bxfitx[i] = fmax(lb[i], ub[i] < bxfitx[i] ? ub[i] : bxfitx[i]);
    }

    int bxx[99];
    memcpy(bxx, bx, sizeof(int) * length);
    double byx[99];
    memcpy(byx, by, sizeof(double) * length);

    double F[88];

    double sum = 0;
    for (int i = 0; i < bxlen; i++) {
        F[i] = bxfitx[0] * expf(bxfitx[1] * bx[i]) + bxfitx[2] * expf(bxfitx[3] * (bx[i] - 88));
        double v1 = (F[i] - by[i]) * (F[i] - by[i]);
        double v2 = sqrtf(inharmon2[i]);
        double v3 = inharmon1[i];
        sum += v1 * v2 * v3;
    }
    return sum;
}

void sortIndexes(int *indices, double *f, int len) {
    for (int i = 0; i < len; i++)
        indices[i] = i;

    int tmp = 0;
    for (int i = 0; i < len; i++) {
        for (int j = (len - 1); j >= (i + 1); j--) {
            if (f[indices[j]] < f[indices[j - 1]]) {
                tmp = indices[j];
                indices[j] = indices[j - 1];
                indices[j - 1] = tmp;
            }
        }
    }
}

double MathUtils::fminsearch(double x[4], double *out, int length, int *bx, double *by, int bxlen,
                             double *inharmon1, double *inharmon2, double *ub, double *lb) {
    int savit = 0, trace = 0, maxiter = length * 200;
    double dirn = -1, tol = 1e-4;
    double stopit[6];
    stopit[0] = tol;
    stopit[1] = (double) maxiter;
    stopit[2] = 2e20;
    stopit[3] = 0;
    stopit[4] = 1;
    stopit[5] = dirn;


    double x0[4];
    memcpy(x0, x, sizeof(double) * 4);
    int n = length;
    Matrix<double> V(n + 1, n);

    for (int i = 0; i < n + 1; i++) {
        for (int k = 0; k < n; k++) {
            if (i - 1 == k)
                V(i, k) = 1;
            else if (i == 0)
                V(i, k) = x0[k];
            else
                V(i, k) = 0;
        }
    }


    double f[5];//n+1];
    memset(f, 0, (n + 1) * sizeof(double));
    f[0] = dirn * fminsearchFunction(x, length, bx, by, bxlen, inharmon1, inharmon2, ub, lb);
    double fmax_old = f[0];

    int k = 0, m = 0;

//## Set up initial simplex.
    double scale = 1;
    for (int i = 0; i < n; i++) {
        if (fabs(x0[i]) > scale)
            scale = fabs(x0[i]);
    }

    double alpha[2];  // is it 1x2 or 1xn+1???
    //scale = max (norm (x0,Inf), 1);
    if (stopit[3] == 0) {
//## Regular simplex - all edges have same length.
//## Generated from construction given in reference [18, pp. 80-81] of [1].
        alpha[0] = scale / (n * sqrtf(2)) * (sqrtf(n + 1) - 1 + n);//, sqrtf(n+1)-1];
        alpha[1] = scale / (n * sqrtf(2)) * (sqrtf(n + 1) - 1);
        for (int i = 1; i < n + 1; i++) {
            for (int k = 0; k < n; k++) {


                V(i, k) = x0[k] + alpha[1];
            }
        }
        for (int i = 1; i < n + 1; i++) {
            if (i > 0)
                V(i, i - 1) = x0[i - 1] + alpha[0];
            for (int k = 0; k < n; k++) {



                //if (i==n)
                x[k] = V(i, k);
            }
            double temp[4];
            memcpy(temp, x, sizeof(double) * 4);
            f[i] = dirn * fminsearchFunction(x, length, bx, by, bxlen, inharmon1, inharmon2, ub,
                                             lb);
        }
    } else {
//## Right-angled simplex based on co-ordinate axes.
        //alpha = scale * ones(n+1,1);
        alpha[0] = scale;
        alpha[1] = scale;
        for (int i = 1; i < n + 1; i++) {
            for (int k = 0; k < n; k++) {


                V(i, k) = x0[k] + scale * V(i, k); //alpha[0];

                if (i == n)
                    x[k] = V(i, k);
            }
            f[i] = dirn *
                   fminsearchFunction(x, length, bx, by, bxlen, inharmon1, inharmon2, ub, lb);
        }
    }
    int nf = n + 1;


    // sort f
    int indices[5];

    sortIndexes(indices, f, n + 1);

    double f2[5];//n+1];
    Matrix<double> V2(n + 1, n);

    for (int i = 0; i < n + 1; i++) {
        f2[i] = f[indices[n - i]];
        for (int k = 0; k < n; k++)
            V2(i, k) = V(indices[n - i], k);
    }
    memcpy(f, f2, sizeof(double) * (n + 1));


    double fx[88];
    memcpy(fx, f, sizeof(double) * (n + 1));
    int indicesx[88];
    memcpy(indicesx, indices, sizeof(int) * (n + 1));
    double xx[88];
    memcpy(xx, x, sizeof(double) * (n + 1));

    V = V2;
    alpha[0] = 1;
    double beta = 0.5f;
    double gamma = 2;
    while (1) {
        k++;

        if (k > maxiter)
            break;

        double fmax = f2[0];
        if (fmax > fmax_old) {
            if (savit != 0) {//! isempty (savit)) {
                for (int i = 0; i < n; i++) {
                    x[i] = V2(0, i);
                }
            }
        }
        fmax_old = fmax;

//## Three stopping tests from MDSMAX.M

//## Stopping Test 1 - f reached target value?
        if (fmax >= stopit[2])
            //  msg = "Exceeded target...quitting\n";
            break;
        //endif

//## Stopping Test 2 - too many f-evals?
        if (nf >= stopit[1])
            //msg = "Max no. of function evaluations exceeded...quitting\n";
            break;
        //endif

//## Stopping Test 3 - converged?   This is test (4.3) in [1].
        double v1[4];//length];
        double normv1 = 0.0f;

        for (int i = 0; i < n; i++) {
            v1[i] = V2(0, i);
            normv1 += fabs(v1[i]);
        }

        if (normv1 < 1)
            normv1 = 1.0f;

        double normV2v1 = 0.0f;
        double sumabsV2v1 = 0.0f;
        for (int i = 1; i < n + 1; i++) {
            sumabsV2v1 = 0;
            for (int k = 0; k < n; k++)
                sumabsV2v1 += fabs(V2(i, k) - v1[k]);

            if (sumabsV2v1 > normV2v1)
                normV2v1 = sumabsV2v1;
        }


        double size_simplex = normV2v1 / normv1;
        if (size_simplex <= tol)
            break;

//##  One step of the Nelder-Mead simplex algorithm
//##  NJH: Altered function calls and changed CNT to NF.
//##       Changed each 'fr < f(1)' type test to '>' for maximization
//##       and re-ordered function values after sort.

        double vbar[4];//n];
        double vr[4];//n];
        for (int k = 0; k < n; k++) {
            vbar[k] = 0.0f;
            for (int i = 0; i < n; i++)
                vbar[k] += V2(i, k);

            vbar[k] = vbar[k] / n;
            vr[k] = (1 + alpha[0]) * vbar[k] - alpha[0] * V2(n, k);
            x[k] = vr[k];
        }

        double fr =
                dirn * fminsearchFunction(x, length, bx, by, bxlen, inharmon1, inharmon2, ub, lb);
        nf = nf + 1;
        double vk[4];//n];
        memcpy(vk, vr, sizeof(double) * n);
        double fk = fr; //how = "reflect, ";
        if (fr > f[n - 1]) {
            if (fr > f[0]) {
                double ve[n];
                for (int i = 0; i < n; i++) {
                    ve[i] = gamma * vr[i] + (1 - gamma) * vbar[i];
                    x[i] = ve[i];
                }

                double fe = dirn *
                            fminsearchFunction(x, length, bx, by, bxlen, inharmon1, inharmon2, ub,
                                               lb);
                nf = nf + 1;
                if (fe > f[0]) {
                    memcpy(vk, ve, sizeof(double) * n);
                    fk = fe;
                }
            }
        } else {
            double vt[4];
            for (int k = 0; k < n; k++)
                vt[k] = V2(n, k);

            double ft = f[n];
            if (fr > ft) {
                memcpy(vt, vr, sizeof(double) * n);
                ft = fr;
            }

            double vc[4];
            for (int k = 0; k < n; k++) {
                vc[k] = beta * vt[k] + (1 - beta) * vbar[k];
                x[k] = vc[k];
            }

            double fc = dirn *
                        fminsearchFunction(x, length, bx, by, bxlen, inharmon1, inharmon2, ub,
                                           lb);
            nf = nf + 1;
            if (fc > f[n - 1]) {
                memcpy(vk, vc, sizeof(double) * n);
                fk = fc;
            } else {
                for (int i = 1; i < n; i++) {
                    for (int k = 0; k < n; k++) {
                        V2(i, k) = (V2(0, k) + V2(i, k)) * 0.5f;
                        x[k] = V2(i, k);
                    }
                    f[i] = dirn *
                           fminsearchFunction(x, length, bx, by, bxlen, inharmon1, inharmon2, ub,
                                              lb);
                }
                nf = nf + n - 1;
                for (int i = 0; i < n; i++) {
                    vk[i] = (V2(0, i) + V2(n, i)) / 2.0f;
                    x[i] = vk[i];
                }
                fk = dirn * fminsearchFunction(x, length, bx, by, bxlen, inharmon1, inharmon2, ub,
                                               lb);
                nf = nf + 1;
            }
        }
        for (int i = 0; i < n; i++) {
            V2(n, i) = vk[i];
        }
        f[n] = fk;

        sortIndexes(indices, f, n + 1);

        for (int i = 0; i < n + 1; i++) {
            f2[i] = f[indices[n - i]];
            for (int k = 0; k < n; k++)
                V(i, k) = V2(indices[n - i], k);
        }

        memcpy(f, f2, sizeof(double) * (n + 1));
        V2 = V;
    }

    memcpy(out, x, sizeof(double) * length);

    return fminsearchFunction(x, length, bx, by, bxlen, inharmon1, inharmon2, ub, lb);
}

bool MathUtils::fMinSearchBnd2(double *x0, double *lb, double *ub, int nPoints, int *bx, double *by,
                               int bxlen, double *inharmon1, double *inharmon2, double *xu,
                               int nValues) {
    int n = nPoints;

    int BoundClass[n];
    for (int i = 0; i < n; i++) {
        int k = 3;
        BoundClass[i] = k;
        if (k == 3 && lb[i] == ub[i])
            BoundClass[i] = 4;
    }

    /*% 0 --> unconstrained variable
     % 1 --> lower bound only
     % 2 --> upper bound only
     % 3 --> dual finite bounds
     % 4 --> fixed variable
     */

    double x0u[4];//n];
    memcpy(x0u, x0, sizeof(double) * n);
    int k = 0;

    for (int i = 0; i < n; i++) {
        //% lower and upper bounds
        if (x0[i] <= lb[i]) {
            //% infeasible starting value
            x0u[k] = -PI / 2;
        } else if (x0[i] >= ub[i]) {
            // % infeasible starting value
            x0u[k] = PI / 2;
        } else {
            x0u[k] = 2 * (x0[i] - lb[i]) / (ub[i] - lb[i]) - 1;
            //% shift by 2*pi to avoid problems at zero in fminsearch
            //% otherwise, the initial simplex is vanishingly small
            x0u[k] = 2 * PI + asin(fmax(-1, x0u[k] > 1.0f ? 1.0f : x0u[k]));
        }

        // % increment k
        k = k + 1;
    }

    if (k < n) {
        n = k;
    }
    fminsearch(x0u, xu, n, bx, by, bxlen, inharmon1, inharmon2, ub, lb);

    for (int i = 0; i < n; i++) {
        xu[i] = (sin(xu[i]) + 1) / 2;
        xu[i] = xu[i] * (ub[i] - lb[i]) + lb[i];
        //% just in case of any doubleing point problems
        xu[i] = fmax(lb[i], ub[i] < xu[i] ? ub[i] : xu[i]);
    }

    return true;
}
