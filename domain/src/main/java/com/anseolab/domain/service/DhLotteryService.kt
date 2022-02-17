package com.anseolab.domain.service

import com.anseolab.domain.model.Lottery
import io.reactivex.rxjava3.core.Single

interface DhLotteryService {
    fun fetchLotteryNumber(drwNo: Long): Single<Lottery>
}