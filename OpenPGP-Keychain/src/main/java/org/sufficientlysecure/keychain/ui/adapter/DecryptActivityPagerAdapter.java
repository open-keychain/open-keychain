package org.sufficientlysecure.keychain.ui.adapter;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.os.DropBoxManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.DecryptActivity;
import org.sufficientlysecure.keychain.ui.DrawerActivity;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DecryptActivityPagerAdapter extends FragmentPagerAdapter implements ActionBar.TabListener, ViewPager.OnPageChangeListener{

    private final Context mContext;
    private final ActionBar mActionBar;
    private final ViewPager mViewPager;
    private DrawerActivity mActivity;
    private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();
    //Dont change the order.
    private final String[] titles={"Decrypt Message", "Decrypt File"};
    static final class TabInfo {
        private final Class<?> clss;
        private Bundle args;
        private final String tag;
        private final int position;

        TabInfo(Class<?> _class, Bundle _args, String _tag) {
            clss = _class;
            args = _args;
            tag = _tag;
            if(DecryptActivity.FRAGMENT_FILE.equals(_tag)){
                position = 1;
            }
            else if(DecryptActivity.FRAGMENT_MESSAGE.equals(_tag)){
                position = 0;
            }
            else{
                position = 0;
            }

        }

        static TabInfo getTabTag(String _tag, ArrayList<TabInfo> mTabs,  Bundle arguments){

           for(int counter=0; counter!=mTabs.size(); counter++){
               TabInfo tab = mTabs.get(counter);
                if(tab.getTag().equals(_tag)){
                    tab.putBundle(arguments);
                    return tab;
                }
            }
            return null;


        }


        public void putBundle(Bundle bundle){
                args = bundle;
        }
        public String getTag(){
            return tag;
        }
        public int getPosition(){
            return position;
        }
    }

    public DecryptActivityPagerAdapter(DrawerActivity activity, ViewPager pager) {
        super(activity.getSupportFragmentManager());
        mContext = activity;
        mActionBar = activity.getSupportActionBar();
        mViewPager = pager;
        mViewPager.setAdapter(this);
        mViewPager.setOnPageChangeListener(this);
    }

    public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args, String Tag, int position) {
        TabInfo info = new TabInfo(clss, args, Tag);
        tab.setTag(info);
        tab.setTabListener(this);

        mTabs.add(info);
        mActionBar.addTab(tab);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mTabs.size();
    }

    @Override
    public Fragment getItem(int position) {
        TabInfo info = mTabs.get(new Integer(position));
        return Fragment.instantiate(mContext, info.clss.getName(), info.args);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        Object tag = tab.getTag();
        for (int i=0; i<mTabs.size(); i++) {
            if (mTabs.get(i) == tag) {
                mViewPager.setCurrentItem(i);
            }
        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        super.destroyItem(container, position, object);
    }

    public void getIntentFromActivity(Bundle arguments, String tag){
            TabInfo tab = TabInfo.getTabTag(tag, mTabs, arguments);
            mViewPager.setCurrentItem(tab.getPosition());
    }


    public void FromActivityResult(){

    }

    public Fragment getCurrentFragment(){
        Fragment page = mActivity.getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.decrypt_pager + ":" + mViewPager.getCurrentItem());
        return page;
    }

    public void setCurrentFragment(String tag){
        TabInfo tab = TabInfo.getTabTag(tag, mTabs, null);
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return titles[position];
    }
}
