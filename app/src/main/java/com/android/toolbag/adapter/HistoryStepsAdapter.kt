package com.android.toolbag.adapter

import com.android.toolbag.R
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class HistoryStepsAdapter: BaseQuickAdapter<Pair<String, String>, BaseViewHolder>(R.layout.detail_history_steps) {
    override fun convert(holder: BaseViewHolder, item: Pair<String, String>) {
        holder.setText(R.id.tv_date, item.first)
        holder.setText(R.id.tv_count, item.second)
    }
}