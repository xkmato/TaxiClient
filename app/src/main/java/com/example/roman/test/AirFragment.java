package com.example.roman.test;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.roman.test.adapters.OrderAdapter;
import com.example.roman.test.data.Order;
import com.example.roman.test.services.SocketService;
import com.google.gson.Gson;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;

import static com.example.roman.test.DetailOrderFragment.DETAIL_ORDER;

public class AirFragment extends Fragment {
    @Inject Gson gson;

    private int mPosition = ListView.INVALID_POSITION;
    private List<Order> mOrders;
    private OrderAdapter mOrderAdapter;
    private static final String SELECTED_KEY = "selected_position";

    static AirFragment newInstance() {
        return new AirFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view =  inflater.inflate(R.layout.fragment_air, container, false);
        ButterKnife.bind(this, view);
        ((TaxiApp) getActivity().getApplication()).getNetComponent().inject(this);

        mOrders = new ArrayList<>();
        mOrderAdapter = new OrderAdapter(getActivity(), mOrders);

        final ListView mListView = (ListView) view.findViewById(R.id.list_view_orders);
        mListView.setAdapter(mOrderAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mPosition = i;
                Order order = mOrders.get(mPosition);
                Intent intent = new Intent(getActivity(), DetailOrderActivity.class);
                intent.putExtra(DETAIL_ORDER, gson.toJson(order));
                startActivity(intent);
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
            mListView.smoothScrollToPosition(mPosition);
        }

        return view;
    }

    public void addOrders(Order[] orders) {
        Collections.addAll(mOrders, orders);
        mOrderAdapter.notifyDataSetChanged();
    }

    public void addOrder(Order order) {

        if (mOrders !=  null) {
            MediaPlayer mp;
            if (order.getIsPrevious()) {
                mp = MediaPlayer.create(getActivity(), R.raw.freeorders);
            } else {
                mp = MediaPlayer.create(getActivity(), R.raw.coldetherorders);
            }
            mp.start();
            mOrders.add(order);
            mOrderAdapter.notifyDataSetChanged();
        }
    }

    public void removeOrder(String id) {
        if (mOrders != null) {
            for (int i = 0; i < mOrders.size(); i++) {
                if (mOrders.get(i).getOrderId().equals(id)) {
                    mOrderAdapter.remove(mOrders.get(i));
                    return;
                }
            }
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            try {
                SocketService.getInstance().getOrders();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            List<Order> orders = savedInstanceState.getParcelableArrayList("orders");
            if (orders != null) {
                addOrders(orders.toArray(new Order[0]));
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }

        outState.putParcelableArrayList("orders", (ArrayList<? extends Parcelable>) mOrders);
        super.onSaveInstanceState(outState);
    }
}
