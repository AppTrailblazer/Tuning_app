#ifndef PIANOTUNING_MATRIX_H
#define PIANOTUNING_MATRIX_H

#include <vector>

template<class T>
class Matrix {
public:
    Matrix(double **data, unsigned int nCols, unsigned int nRows) :
            m_nRows(nRows),
            m_nCols(nCols),
            m_oData(nRows * nCols, 0) {
        for (int r = 0; r < nRows; r++) {
            for (int c = 0; c < nCols; c++) {
                m_oData[c + m_nCols * r] = data[r][c];
            }
        }
    }

    Matrix(unsigned int nRows, unsigned int nCols) :
            m_nRows(nRows),
            m_nCols(nCols),
            m_oData(nRows * nCols, 0) {
    }

    static Matrix identity(unsigned int nSize) {
        Matrix oResult(nSize, nSize);

        int nCount = 0;
        std::generate(oResult.m_oData.begin(), oResult.m_oData.end(),
                      [&nCount, nSize]() { return !(nCount++ % (nSize + 1)); });

        return oResult;
    }

    inline T &operator()(unsigned int nRow, unsigned int nCol) {
        return m_oData[nCol + m_nCols * nRow];
    }

    inline Matrix operator*(Matrix &other) {
        Matrix oResult(m_nRows, other.m_nCols);
        for (unsigned int r = 0; r < m_nRows; ++r) {
            for (unsigned int ocol = 0; ocol < other.m_nCols; ++ocol) {
                for (unsigned int c = 0; c < m_nCols; ++c) {
                    oResult(r, ocol) += (*this)(r, c) * other(c, ocol);
                }
            }
        }

        return oResult;
    }

    inline Matrix transpose() {
        Matrix oResult(m_nCols, m_nRows);
        for (unsigned int r = 0; r < m_nRows; ++r) {
            for (unsigned int c = 0; c < m_nCols; ++c) {
                oResult(c, r) += (*this)(r, c);
            }
        }
        return oResult;
    }

    inline unsigned int rows() {
        return m_nRows;
    }

    inline unsigned int cols() {
        return m_nCols;
    }

    inline std::vector<T> data() {
        return m_oData;
    }

private:
    std::vector<T> m_oData;

    unsigned int m_nRows;
    unsigned int m_nCols;
};

typedef Matrix<double> DoubleMatrix;
typedef Matrix<float> FloatMatrix;

#endif //PIANOTUNING_MATRIX_H
