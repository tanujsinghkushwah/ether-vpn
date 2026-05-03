package com.anonymous.ethervpn.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.anonymous.ethervpn.interfaces.NavItemClickListener;
import com.anonymous.ethervpn.model.Server;
import com.anonymous.ethervpn.utilities.FlagResolver;
import com.bumptech.glide.Glide;
import com.anonymous.ethervpn.R;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;

public class ServerListRVAdapter extends RecyclerView.Adapter<ServerListRVAdapter.MyViewHolder> {

    private ArrayList<Server> serverLists;
    private Context mContext;
    private NavItemClickListener listener;

    public ServerListRVAdapter(ArrayList<Server> serverLists, Context context) {
        this.serverLists = serverLists;
        this.mContext = context;
        this.listener = (NavItemClickListener) context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.server_list_view, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Server server = serverLists.get(position);
        holder.serverCountry.setText(server.getCountry());

        int flagResId = FlagResolver.resolve(server.getCountry());
        if (flagResId != 0) {
            holder.serverIcon.setImageResource(flagResId);
        } else if (server.getFlagUrl() != null) {
            Glide.with(mContext)
                    .load(server.getFlagUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.serverIcon);
        } else {
            holder.serverIcon.setImageResource(R.drawable.flag_uk);
        }

        holder.serverItemLayout.setOnClickListener(v -> listener.clickedItem(position));
    }

    @Override
    public int getItemCount() {
        return serverLists.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout serverItemLayout;
        ImageView serverIcon;
        TextView serverCountry;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            serverItemLayout = itemView.findViewById(R.id.serverItemLayout);
            serverIcon = itemView.findViewById(R.id.iconImg);
            serverCountry = itemView.findViewById(R.id.countryTv);
        }
    }
}
