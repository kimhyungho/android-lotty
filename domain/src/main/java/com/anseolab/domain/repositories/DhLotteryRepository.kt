package com.anseolab.domain.repositories

import com.anseolab.domain.model.Lottery
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single

interface DhLotteryRepository {
    fun fetchLotteryNumber(drwNo: Long): Single<Lottery>

    fun searchLotteryNumber(drwNo: Long): Single<Lottery>

    fun fetchRecentLotteries(): Flowable<List<Lottery>>

    fun deleteLottery(drwNo: Long): Completable

    fun clearLotteries(): Completable
}