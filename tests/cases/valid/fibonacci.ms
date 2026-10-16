laFaena pavos fibRec(pavos n) {
    fijateSi (n <= 1) {
        suelta n;
    } ySiNo {
        suelta fibRec(n - 1) + fibRec(n - 2);
    }
}

laFaena pavos fibIter(pavos n) {
    fijateSi (n <= 1) {
        suelta n;
    } ySiNo {
        unTal pavos a = 0;
        unTal pavos b = 1;
        unTal pavos i = 2;
        unTal pavos tmp = 0;

        segunVea (i <= n) {
            tmp = a + b;
            a = b;
            b = tmp;
            i = i + 1;
        }

        suelta b;
    }
}

laFaena naDeNa main() {
    unTal pavos recursivo = 0;
    unTal pavos iterativo = 0;

    recursivo = fibRec(6);
    iterativo = fibIter(6);
}
