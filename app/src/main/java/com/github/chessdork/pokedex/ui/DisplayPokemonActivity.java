package com.github.chessdork.pokedex.ui;

import android.animation.ValueAnimator;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.github.chessdork.pokedex.R;
import com.github.chessdork.pokedex.common.PokeDatabase;
import com.github.chessdork.pokedex.models.Ability;
import com.github.chessdork.pokedex.models.Pokemon;
import com.github.chessdork.pokedex.models.PokemonType;

import java.util.ArrayList;
import java.util.List;


public class DisplayPokemonActivity extends ActionBarActivity {
    public static final String POKEMON_OBJECT = "POKEMON_OBJECT";

    private static final int STAT_MAX_SCALE = 150;
    private static final long ANIM_DURATION = 300;
    private static final int[] ALL_STAT_BARS = {
            R.id.hp_rectangle,
            R.id.patk_rectangle,
            R.id.pdef_rectangle,
            R.id.spatk_rectangle,
            R.id.spdef_rectangle,
            R.id.spe_rectangle
    };

    // if the activity is stopped before the moveset parsing finishes, we should kill the AsyncTask
    // since the result is no longer useful.
    private AsyncTask mTask;
    private Pokemon mPokemon;
    private boolean statBarsSetup = false;
    private int id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_pokemon);
        mPokemon = (Pokemon) getIntent().getSerializableExtra(POKEMON_OBJECT);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        PokeDatabase db = PokeDatabase.getInstance(this);
        String query = "select pokemon.national_id from pokemon where pokemon.name like ?";
        Cursor c = db.getReadableDatabase().rawQuery(query, new String[] { mPokemon.getName() });
        c.moveToFirst();
        id = c.getInt(0);
        System.out.println(id);

        setupStaticUi();
        setupListView();
    }

    /**
     * Setup views that don't rely on the result of the AsyncTask.
     */
    @SuppressWarnings("deprecation")
    private void setupStaticUi() {
        TextView textView = (TextView) findViewById(R.id.pokemon_name);
        textView.setText(mPokemon.getName());

        TextView type1 = (TextView) findViewById(R.id.pokemon_type1);
        TextView type2 = (TextView) findViewById(R.id.pokemon_type2);
        List<PokemonType> types = mPokemon.getTypes();
        if (types.size() == 1) {
            type1.setVisibility(View.VISIBLE);
            type1.setText(types.get(0).getName());
            type1.setBackgroundDrawable(types.get(0).createGradient());

            type2.setVisibility(View.INVISIBLE);
        } else if (types.size() == 2) {
            type1.setVisibility(View.VISIBLE);
            type1.setText(types.get(0).getName());
            type1.setBackgroundDrawable(types.get(0).createLeftGradient());

            type2.setVisibility(View.VISIBLE);
            type2.setText(types.get(1).getName());
            type2.setBackgroundDrawable(types.get(1).createRightGradient());
        }

        TextView tags = (TextView) findViewById(R.id.pokemon_tags);
        tags.setText(mPokemon.getTag());

        ImageView imageView = (ImageView) findViewById(R.id.image_view);
        if (imageView != null) {
            imageView.setImageResource(getResources().getIdentifier("ic_pokemon_" + id, "drawable", getPackageName()));
        }
    }

    /**
     * Populate the ListView after the AsyncTask has completed
     */
    private void setupListView() {
        ListView listView = (ListView) findViewById(R.id.moveset_list);
        List<Ability> abilities = new ArrayList<>();

        PokeDatabase db = PokeDatabase.getInstance(this);
        Cursor c = db.getReadableDatabase().rawQuery("select abilities.id, abilities.name, abilities.description from pokemon_abilities join abilities on abilities.id = ability_id join pokemon on pokemon.id = pokemon_id where pokemon.name=?", new String[] {mPokemon.getName()});
        while (c.moveToNext()) {
            abilities.add(new Ability(c));
        }
        c.close();
        listView.setAdapter(new DisplayAbilitiesFragment.AbilitiesAdapter(this, abilities));
        listView.setEmptyView(findViewById(R.id.empty_text));

        //hide the progress bar now that the ListView is populated
        findViewById(R.id.progress_bar).setVisibility(View.GONE);
        //findViewById(R.id.new_moveset_button).setVisibility(View.VISIBLE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
            mTask.cancel(true);
            mTask = null;
        }
    }

    public void createMoveset(View view) {
        //Intent intent = new Intent(this, CreateMovesetActivity.class);
        //startActivity(intent);
    }

    /**
     * Resize stat bars to fit the screen once they are created and begin animation.
     *
     * @param hasFocus whether the window has focus
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            if (!statBarsSetup) {
                // only do this once per orientation
                statBarsSetup = true;
                setupStatBar(R.id.hp_rectangle, R.id.hp_stat, mPokemon.getHp());
                setupStatBar(R.id.patk_rectangle, R.id.patk_stat, mPokemon.getAttack());
                setupStatBar(R.id.pdef_rectangle, R.id.pdef_stat, mPokemon.getDefense());
                setupStatBar(R.id.spatk_rectangle, R.id.spatk_stat, mPokemon.getSpecialAttack());
                setupStatBar(R.id.spdef_rectangle, R.id.spdef_stat, mPokemon.getSpecialDefense());
                setupStatBar(R.id.spe_rectangle, R.id.spe_stat, mPokemon.getSpeed());
            }

            ValueAnimator statAnim = ValueAnimator.ofFloat(0f, 1f);
            statAnim.setDuration(ANIM_DURATION);
            statAnim.addUpdateListener(new BarAnimatorUpdateListener(ALL_STAT_BARS));
            statAnim.start();
        }
    }

    /**
     * After the first layout pass is finished, dynamically size the stat bars based on
     * available screen width.
     *
     * @param rectId    resource id for rectangle shape
     * @param textId    resource id for TextView
     * @param statValue stat
     */
    private void setupStatBar(int rectId, int textId, int statValue) {
        TextView stat = (TextView) findViewById(textId);
        stat.setText(String.valueOf(statValue));

        View rectangle = findViewById(rectId);
        LayerDrawable layer = (LayerDrawable) rectangle.getBackground();
        int color = createColorFromStat(statValue);
        layer.findDrawableByLayerId(R.id.stat_color).setColorFilter(color, PorterDuff.Mode.SRC_OVER);

        int start = rectangle.getLeft();
        int end = rectangle.getRight();
        float scale = (end - start) / (float) STAT_MAX_SCALE;

        ViewGroup.LayoutParams params = rectangle.getLayoutParams();
        params.width = (int) (Math.min(statValue, STAT_MAX_SCALE) * scale);
        rectangle.setLayoutParams(params);
    }

    /**
     * Animate the stat bar width from 0 to 100%.
     */
    private class BarAnimatorUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        private final int[] ids;
        private final int[] finalWidths;

        public BarAnimatorUpdateListener(int... rectIds) {
            ids = rectIds;
            finalWidths = new int[ids.length];

            for (int i = 0; i < finalWidths.length; i++) {
                finalWidths[i] = findViewById(ids[i]).getLayoutParams().width;
            }
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            float f = (Float) valueAnimator.getAnimatedValue();

            for (int i = 0; i < ids.length; i++) {
                View view = findViewById(ids[i]);
                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.width = (int) (finalWidths[i] * f);
                view.setLayoutParams(params);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.display_pokemon, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Magic function to get stat colors.
     *
     * @param stat the stat
     * @return the color as an integer
     */
    private int createColorFromStat(int stat) {
        int n = (int) Math.floor(2.55 * Math.min(Math.max(stat - 50, 0), 100));
        int r = Math.min(2 * (255 - n), 255);
        int g = Math.min(2 * n, 255);
        int b = (int) Math.floor(4.25 * Math.min(Math.max(stat - 140, 0), 60));
        return Color.rgb(r, g, b);
    }
}
