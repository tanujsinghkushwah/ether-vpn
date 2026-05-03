package com.anonymous.ethervpn.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.anonymous.ethervpn.R;
import com.anonymous.ethervpn.model.Server;
import com.anonymous.ethervpn.utilities.FlagResolver;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class ServerListV2Adapter extends RecyclerView.Adapter<ServerListV2Adapter.VH> {

    public interface OnServerSelectedListener {
        void onServerSelected(int index, Server server);
    }

    private final Context context;
    private List<Server> fullList;
    private List<Server> displayList;
    private int selectedIndex = -1;
    private OnServerSelectedListener listener;

    public ServerListV2Adapter(Context context, List<Server> servers) {
        this.context = context;
        this.fullList = new ArrayList<>(servers);
        this.displayList = new ArrayList<>(servers);
    }

    public void setOnServerSelectedListener(OnServerSelectedListener l) {
        this.listener = l;
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
        notifyDataSetChanged();
    }

    public void filter(String query, String tab) {
        displayList.clear();
        for (Server s : fullList) {
            boolean matchesTab = matchesTab(s, tab);
            boolean matchesQuery = query.isEmpty()
                    || s.getCountry().toLowerCase().contains(query.toLowerCase())
                    || s.getCity().toLowerCase().contains(query.toLowerCase());
            if (matchesTab && matchesQuery) {
                displayList.add(s);
            }
        }
        notifyDataSetChanged();
    }

    private boolean matchesTab(Server s, String tab) {
        switch (tab) {
            case "free": return !s.isPremium();
            case "premium": return s.isPremium();
            case "favorites": return s.isFavorite();
            default: return true;
        }
    }

    public Server getItem(int position) {
        return displayList.get(position);
    }

    public int getFullIndexOf(Server server) {
        return fullList.indexOf(server);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_server_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Server server = displayList.get(position);
        int fullIdx = getFullIndexOf(server);

        holder.country.setText(server.getCountry());

        String city = server.getCity();
        if (city != null && !city.isEmpty()) {
            holder.city.setText(city);
            holder.city.setVisibility(View.VISIBLE);
        } else {
            holder.city.setVisibility(View.GONE);
        }

        int flagRes = FlagResolver.resolve(server.getIsoCode().isEmpty()
                ? server.getCountry() : server.getIsoCode());
        if (flagRes != 0) {
            holder.flag.setImageResource(flagRes);
        } else if (server.getFlagUrl() != null && !server.getFlagUrl().isEmpty()) {
            Glide.with(context).load(server.getFlagUrl())
                    .placeholder(R.drawable.flag_uk)
                    .into(holder.flag);
        } else {
            holder.flag.setImageResource(R.drawable.flag_uk);
        }

        holder.premiumBadge.setVisibility(server.isPremium() ? View.VISIBLE : View.GONE);

        holder.favBtn.setImageResource(server.isFavorite()
                ? R.drawable.ic_star_filled : R.drawable.ic_star);
        holder.favBtn.setOnClickListener(v -> {
            server.setFavorite(!server.isFavorite());
            notifyItemChanged(holder.getAdapterPosition());
        });

        int ping = server.getPingMs();
        if (ping >= 0) {
            holder.ping.setText(ping + " ms");
            holder.ping.setVisibility(View.VISIBLE);
        } else {
            holder.ping.setVisibility(View.GONE);
        }

        int load = server.getLoad();
        if (load >= 0) {
            holder.loadBarContainer.setVisibility(View.VISIBLE);
            int width = (int) (holder.loadBarContainer.getLayoutParams().width * (load / 100f));
            ViewGroup.LayoutParams lp = holder.loadBarFill.getLayoutParams();
            lp.width = width;
            holder.loadBarFill.setLayoutParams(lp);
        } else {
            holder.loadBarContainer.setVisibility(View.GONE);
        }

        holder.selected.setVisibility(fullIdx == selectedIndex ? View.VISIBLE : View.INVISIBLE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onServerSelected(fullIdx, server);
            }
        });
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView flag, selected;
        TextView country, city, ping, premiumBadge;
        ImageButton favBtn;
        FrameLayout loadBarContainer;
        View loadBarFill;

        VH(@NonNull View v) {
            super(v);
            flag = v.findViewById(R.id.rowFlag);
            country = v.findViewById(R.id.rowCountry);
            city = v.findViewById(R.id.rowCity);
            ping = v.findViewById(R.id.rowPing);
            premiumBadge = v.findViewById(R.id.rowPremiumBadge);
            favBtn = v.findViewById(R.id.rowFavBtn);
            selected = v.findViewById(R.id.rowSelected);
            loadBarContainer = v.findViewById(R.id.rowLoadBarContainer);
            loadBarFill = v.findViewById(R.id.rowLoadBarFill);
        }
    }
}
