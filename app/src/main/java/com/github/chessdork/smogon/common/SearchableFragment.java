package com.github.chessdork.smogon.common;


import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import com.github.chessdork.smogon.R;

/**
 * Utility fragment for use with a SearchView activity with Navigation Drawers.
 * Subclasses should use setAdapter() to use the default SearchView handling.
 */
public class SearchableFragment extends Fragment {
    private String TAG = getClass().getSimpleName();

    private FilterableAdapter adapter;

    public void setFilterableAdapter(FilterableAdapter adapter) {
        this.adapter = adapter;
    }

    public void hideSoftKeyboard() {
        // Hide soft keyboard, if visible
        if (getView() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager)
                    getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchItem.setOnActionExpandListener(new MenuCollapseListener());

        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new QueryTextListener());
    }

    /**
     * Resets the list when the SearchView is collapsed and closes the navigation drawer
     * when the SearchView is expanded.
     */
    private class MenuCollapseListener implements MenuItem.OnActionExpandListener {
        @Override
        public boolean onMenuItemActionExpand(MenuItem menuItem) {
            // close the navigation drawer on search.
            DrawerLayout drawer = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
            drawer.closeDrawers();
            return true;
        }

        @Override
        public boolean onMenuItemActionCollapse(MenuItem menuItem) {
            adapter.getFilter().filter("");
            return true;
        }
    }

    /**
     * Filters the list on text change.
     */
    private class QueryTextListener implements SearchView.OnQueryTextListener {
        @Override
        public boolean onQueryTextSubmit(String s) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            Log.d(TAG, "query: " + s);
            if (adapter != null) {
                adapter.getFilter().filter(s);
            }
            return false;
        }
    }
}
