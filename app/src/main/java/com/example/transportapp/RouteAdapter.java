package com.example.transportapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.RouteViewHolder> {

    private List<Route> routeList;
    private OnRouteClickListener listener;

    public RouteAdapter(List<Route> routeList, OnRouteClickListener listener) {
        this.routeList = routeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route, parent, false);
        return new RouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        Route route = routeList.get(position);
        holder.routeNameTextView.setText(route.getRouteName());
        holder.itemView.setOnClickListener(v -> listener.onRouteClick(route));
    }

    @Override
    public int getItemCount() {
        return routeList.size();
    }

    static class RouteViewHolder extends RecyclerView.ViewHolder {
        TextView routeNameTextView;

        public RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            routeNameTextView = itemView.findViewById(R.id.routeNameTextView);
        }
    }

    public interface OnRouteClickListener {
        void onRouteClick(Route route);
    }
}

