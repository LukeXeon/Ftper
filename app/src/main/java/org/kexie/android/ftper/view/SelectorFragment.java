package org.kexie.android.ftper.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;
import com.flyco.tablayout.listener.OnTabSelectListener;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUITipDialog;
import org.kexie.android.ftper.R;
import org.kexie.android.ftper.databinding.FragmentSelectorBinding;
import org.kexie.android.ftper.viewmodel.SelectorViewModel;
import org.kexie.android.ftper.viewmodel.bean.FileItem;
import org.kexie.android.ftper.widget.FilePagerAdapter;
import org.kexie.android.ftper.widget.Utils;


public class SelectorFragment extends Fragment {

    private SelectorViewModel mViewModel;

    private FilePagerAdapter mFilePagerAdapter;

    private FragmentSelectorBinding mBinding;

    private QMUITipDialog dialog = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFilePagerAdapter = new FilePagerAdapter(this,
                (adapter, view1, position) -> {
                    FileItem fileItem = (FileItem) adapter.getItem(position);
                    if (fileItem != null) {
                        new QMUIDialog.MessageDialogBuilder(requireContext())
                                .setTitle("提示")
                                .setMessage("确定要上传该文件吗？")
                                .addAction("取消", (dialog1, index1) -> dialog1.dismiss())
                                .addAction("确认", (dialog12, index12) -> {
                                    mViewModel.select(fileItem);
                                    dialog12.dismiss();
                                })
                                .create(com.qmuiteam.qmui.R.style.QMUI_Dialog)
                                .show();
                    }
                });
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_selector,
                container,
                false);
        mBinding.getRoot().setOnTouchListener((v, event) -> true);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = ViewModelProviders.of(requireActivity())
                .get(SelectorViewModel.class);

        mBinding.tabs.setTabData(new String[]{
                getString(R.string.image),
                getString(R.string.word),
                getString(R.string.xls),
                getString(R.string.ppt),
                getString(R.string.pdf)
        });

        mBinding.tabs.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelect(int position) {
                mBinding.pager.setCurrentItem(position);
            }

            @Override
            public void onTabReselect(int position) {

            }
        });

        mBinding.pager.setOffscreenPageLimit(4);

        mBinding.pager.addOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        mBinding.tabs.setCurrentTab(position);
                        mViewModel.loadData(position);
                    }
                });

        mBinding.pager.setAdapter(mFilePagerAdapter);

        for (int i = 0; i < 5; i++) {
            final int position = i;
            mViewModel.getPagerData(position).observe(this,
                    data -> mFilePagerAdapter.setData(position, data));
        }

        mViewModel.isLoading().observe(this,
                isLoading -> {
                    if (isLoading && dialog == null) {
                        dialog = new QMUITipDialog
                                .Builder(requireContext())
                                .setIconType(QMUITipDialog.Builder.ICON_TYPE_LOADING)
                                .setTipWord(getString(R.string.loading))
                                .create();
                        dialog.setCancelable(false);
                        dialog.show();
                    } else {
                        if (dialog != null) {
                            dialog.dismiss();
                            dialog = null;
                        }
                    }
                });

        Utils.subscribe(this,
                mViewModel.getOnSelect(),
                Lifecycle.Event.ON_DESTROY,
                file -> {
                    Fragment fragment = getTargetFragment();
                    if (fragment != null) {
                        Intent intent = new Intent();
                        intent.putExtra(getString(R.string.file), file);
                        fragment.onActivityResult(R.id.open_select_request_code,
                                Activity.RESULT_OK,
                                intent);
                    }
                    requireActivity().onBackPressed();
                });

        mViewModel.loadData(mBinding.pager.getCurrentItem());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFilePagerAdapter = null;
    }
}