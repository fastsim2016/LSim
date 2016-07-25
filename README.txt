Requirement
===========
JAMA library needed.
We used jama-1.0.2.jar from http://math.nist.gov/javanumerics/jama/

How to Run
==========

We can run LinearizedSimRank in 2 steps:

Step 1: precompute D: baseline.exec -> OfflineStage.java

Step 2: online query processing:
single-pair: baseline.exec -> SinglePairOnline.java
single-source: baseline.exec -> SingleSourceOnline.java

Note that, all the parameters are specified in config.properties file.

More notes
==========

1. OfflineStage.java: offline pre computation
 corresponding to alg. 4 and alg. 5 in the paper.
             offline.algorithm4(L, graph, T, R, c, D); //alg. 4 in the paper, which will call alg. 5
 result5=algorithm5(T, k, R, c, D, G);

2.OnlineStage.java: Online processing-- including alg. 1, 2 and 3 in the paper.
 public double singlePairSimRank(int T,int i,int j,int n,double c,Matrix D,Matrix P) //single-pair algorithm 1
  public double[] singleSourceSimRank(int T,int i,int n,double c,Matrix D,Matrix P) //single-source algorithm 2

Note that, for these two methods, we also have the optimized version singlePairSimRankOptimized and singleSourceSimRankOptimized (which is our own optimization), but the experiments shows that such optimization doesn't improve the efficiency much. So we still use the original methods as described in the paper.

3. SinglePairOnline.jave and SingleSourceOnline.java: each handling one simrank mode

4. Config.java: parameters we used; you can find the description of each parameter in config.properties. Generally, we use the same notations as in the paper to name corresponding variables. 