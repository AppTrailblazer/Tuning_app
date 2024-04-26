#ifndef MathUtils_h
#define MathUtils_h

#include <stdio.h>
#include <string.h>
#include <complex>
#include <algorithm>
#include <iterator>

typedef std::complex<double> Complex;


class FSumSquareFunction {
    int *bx;
    double *by;
    double *inharmon1;
    double *inharmon2;
    int datalen = 0;
public:
    FSumSquareFunction(int *bx, double *by, double *inharmon1, double *inharmon2, int _datalen) {
        this->bx = bx;
        this->by = by;
        this->inharmon1 = inharmon1;
        this->inharmon2 = inharmon2;
        datalen = _datalen;
    }

    inline void myFunc(double *bxfit, int *xdata, int xdatalength, double *out) {
        for (int i = 0; i < xdatalength; i++) {
            out[i] = bxfit[0] * exp(bxfit[1] * xdata[i]) +
                     bxfit[2] * exp(bxfit[3] * (xdata[i] - 88));
        }
    }

    inline double value(double *bxfit) {
        double F[88];

        myFunc(bxfit, bx, datalen, F);
        double sum = 0;
        for (int i = 0; i < datalen; i++) {
            double v1 = (F[i] - by[i]) * (F[i] - by[i]);
            double v2 = sqrtf(inharmon2[i]);
            double v3 = inharmon1[i];
            sum += v1 * v2 * v3;
        }
        return sum;
    }

};


class MathUtils {
public:
    MathUtils();

    static float log2(float x) { return logf(x) / 0.6931471805599453f; }

    static void deriv(double *in, double *out, int n);

    static double mean(double *in, int start, int end);

    static void fastsmooth(double *in, double *out, double w, int type, int ends, int L);

    static int sgn(double val);

    template<typename T>
    static T median(T *in, int n) {
        T sorted[n];
        memcpy(sorted, in, n * sizeof(T));
        std::sort(sorted, sorted + n);
        return (n % 2) == 0 ? (sorted[n / 2] + sorted[(n / 2) - 1]) * 0.5 : sorted[n / 2];
    }

    template<typename T>
    static typename T::value_type median(T in) {
        int n = in.size();
        std::sort(std::begin(in), std::end(in));
        return (n % 2) == 0 ? (in[n / 2] + in[(n / 2) - 1]) * 0.5 : in[n / 2];
    }

    static double variance(double *in, int n);

    static double std(double *in, int n);

    static void polyVal(double *p, int pLen, double *x, int xLen, double *res);

    static void polyFit(double *x, int ndata, double *y, int order, double *a);

    static double
    fmin(double a, double b, double tol, int maxValFun, float *signal, int size, Complex *grid,
         Complex *gridSignal, int method);

    static double
    fmin(double a, double b, double tol, int maxValFun, float *signal, int size, double *grid,
         int method);

    static double fminsearch(double val[4], double *out, int length, int *bx, double *by, int bxlen,
                             double *inharmon1, double *inharmon2, double *ub, double *lb);

    static bool
    fMinSearchBnd2(double *x0, double *lb, double *ub, int n, int *bx, double *by, int bxlen,
                   double *inharmon1, double *inharmon2, double *xu, int v);

    static void solveCholeskyMatrix(int i, double in[87][87], double *p, double *out);

    static void determineCholeskyMatrix(int i, double in[87][87], double out[87][87]);

    static double
    fminsearchFunction(double *bxfit, int length, int *bx, double *by, int bxlen, double *inharmon1,
                       double *inharmon2, double *ub, double *lb);

private:
    static void sa(double *Y, double *s, double smoothwidth, int ends, int L);

    static double
    zeroGoal(float *signal, int size, double frequency, Complex *grid, Complex *gridSignal);

    static double optimalGoal(float *signal, int size, double frequency, Complex *grid);

    static double optimalGoal(float *signal, int size, double frequency, double *grid);

    static double multiplyVectors(double *a, float *b, int size);

};

#endif /* MathUtils_h */
