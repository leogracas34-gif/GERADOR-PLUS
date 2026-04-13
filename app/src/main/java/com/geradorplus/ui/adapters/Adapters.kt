package com.geradorplus.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.geradorplus.R
import com.geradorplus.data.models.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip

// ==================== CONTENT CARD ADAPTER (TMDB) ====================

class ContentCardAdapter(
    private var items: List<TmdbContent>,
    private var imageBaseUrl: String,
    private val onClick: (TmdbContent) -> Unit
) : RecyclerView.Adapter<ContentCardAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPoster: ImageView = view.findViewById(R.id.ivPoster)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvYear: TextView = view.findViewById(R.id.tvYear)
        val tvRating: TextView = view.findViewById(R.id.tvRating)
        val card: MaterialCardView = view.findViewById(R.id.card)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_content_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.getDisplayTitle()
        holder.tvYear.text = item.getYear()
        holder.tvRating.text = "★ ${String.format("%.1f", item.voteAverage ?: 0f)}"

        Glide.with(holder.itemView.context)
            .load(imageBaseUrl + (item.posterPath ?: ""))
            .placeholder(R.drawable.placeholder_poster)
            .error(R.drawable.placeholder_poster)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.ivPoster)

        holder.card.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<TmdbContent>) {
        items = newItems
        notifyDataSetChanged()
    }

    // ✅ CORRIGIDO: método adicionado para atualizar lista e URL base juntos
    fun updateDataWithBase(newItems: List<TmdbContent>, newImageBaseUrl: String) {
        items = newItems
        imageBaseUrl = newImageBaseUrl
        notifyDataSetChanged()
    }
}

// ==================== BANNER LIST ADAPTER ====================

class BannerListAdapter(
    private val onShare: (Banner) -> Unit,
    private val onDelete: (Banner) -> Unit
) : ListAdapter<Banner, BannerListAdapter.ViewHolder>(BannerDiffCallback()) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivBanner: ImageView = view.findViewById(R.id.ivBanner)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvType: TextView = view.findViewById(R.id.tvType)
        val btnShare: MaterialButton = view.findViewById(R.id.btnShare)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_banner, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val banner = getItem(position)
        holder.tvTitle.text = banner.title
        holder.tvDate.text = java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault())
            .format(java.util.Date(banner.createdAt))
        holder.tvType.text = when (banner.type) {
            BannerType.MOVIE_LAUNCH -> "Filme"
            BannerType.SERIES_LAUNCH -> "Série"
            BannerType.PROMOTION -> "Promoção"
            BannerType.TRAILER -> "Trailer"
            BannerType.IPTV_SERVICE -> "IPTV"
            BannerType.CUSTOM -> "Custom"
        }

        Glide.with(holder.itemView.context)
            .load(banner.outputPath)
            .placeholder(R.drawable.placeholder_banner)
            .error(R.drawable.placeholder_banner)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.ivBanner)

        holder.btnShare.setOnClickListener { onShare(banner) }
        holder.btnDelete.setOnClickListener { onDelete(banner) }
    }

    class BannerDiffCallback : DiffUtil.ItemCallback<Banner>() {
        override fun areItemsTheSame(oldItem: Banner, newItem: Banner) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Banner, newItem: Banner) = oldItem == newItem
    }
}

// ==================== SEARCH RESULT ADAPTER ====================

class SearchResultAdapter(
    private var imageBaseUrl: String,
    private val onClick: (TmdbContent) -> Unit
) : ListAdapter<TmdbContent, SearchResultAdapter.ViewHolder>(SearchDiffCallback()) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPoster: ImageView = view.findViewById(R.id.ivPoster)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvYear: TextView = view.findViewById(R.id.tvYear)
        val tvType: Chip = view.findViewById(R.id.chipType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.tvTitle.text = item.getDisplayTitle()
        holder.tvYear.text = item.getYear()
        holder.tvType.text = if (item.isMovie()) "Filme" else "Série"

        Glide.with(holder.itemView.context)
            .load(imageBaseUrl + (item.posterPath ?: ""))
            .placeholder(R.drawable.placeholder_poster)
            .into(holder.ivPoster)

        holder.itemView.setOnClickListener { onClick(item) }
    }

    // ✅ CORRIGIDO: método adicionado para atualizar lista e URL base juntos
    fun updateWithBase(newItems: List<TmdbContent>, newImageBaseUrl: String) {
        imageBaseUrl = newImageBaseUrl
        submitList(newItems)
    }

    class SearchDiffCallback : DiffUtil.ItemCallback<TmdbContent>() {
        override fun areItemsTheSame(oldItem: TmdbContent, newItem: TmdbContent) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TmdbContent, newItem: TmdbContent) = oldItem == newItem
    }
}

// ==================== USER LIST ADAPTER ====================

class UserListAdapter(
    private val onRenew: (User) -> Unit,
    private val onSuspend: (User) -> Unit,
    private val onActivate: (User) -> Unit,
    private val onDelete: (User) -> Unit
) : ListAdapter<User, UserListAdapter.ViewHolder>(UserDiffCallback()) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvExpiry: TextView = view.findViewById(R.id.tvExpiry)
        val tvActivated: TextView = view.findViewById(R.id.tvActivated)
        val tvContact: TextView = view.findViewById(R.id.tvContact)
        val chipLicense: Chip = view.findViewById(R.id.chipLicense)
        val btnRenew: MaterialButton = view.findViewById(R.id.btnRenew)
        val btnSuspend: MaterialButton = view.findViewById(R.id.btnSuspend)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnDelete)
        val card: MaterialCardView = view.findViewById(R.id.card)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = getItem(position)
        val ctx = holder.itemView.context

        holder.tvUsername.text = user.username
        holder.tvStatus.text = user.getStatusLabel()
        holder.tvContact.text = user.contactInfo ?: "-"

        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        holder.tvActivated.text = "Ativado: ${if (user.activatedAt > 0) sdf.format(java.util.Date(user.activatedAt)) else "N/A"}"
        holder.tvExpiry.text = "Expira: ${if (user.expiresAt > 0) sdf.format(java.util.Date(user.expiresAt)) else "N/A"}"

        holder.chipLicense.text = when (user.licenseType) {
            LicenseType.TRIAL -> "TESTE"
            LicenseType.MONTHLY -> "${user.getRemainingDays()}d"
            LicenseType.NONE -> "SEM LICENÇA"
        }

        val statusColor = when {
            user.licenseStatus == LicenseStatus.SUSPENDED -> 0xFFFF6B6B.toInt()
            user.isExpired() -> 0xFFFF9800.toInt()
            user.licenseType == LicenseType.TRIAL -> 0xFF00BCD4.toInt()
            else -> 0xFF4CAF50.toInt()
        }
        holder.tvStatus.setTextColor(statusColor)

        holder.card.strokeColor = statusColor
        holder.card.strokeWidth = if (user.isExpired() || user.licenseStatus == LicenseStatus.SUSPENDED) 3 else 0

        holder.btnRenew.setOnClickListener { onRenew(user) }
        holder.btnDelete.setOnClickListener { onDelete(user) }

        if (user.licenseStatus == LicenseStatus.SUSPENDED) {
            holder.btnSuspend.text = "Ativar"
            holder.btnSuspend.setOnClickListener { onActivate(user) }
        } else {
            holder.btnSuspend.text = "Suspender"
            holder.btnSuspend.setOnClickListener { onSuspend(user) }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
    }
}
