package com.anseolab.domain.service

import com.anseolab.domain.model.KakaoStore
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single


interface KakaoService {
    fun search(query: String?, x: Double, y: Double, type: String): Single<List<KakaoStore>>

    fun clear(): Completable

    fun remove(address: String): Completable

    fun fetchAddress(): Flowable<List<String>>
}