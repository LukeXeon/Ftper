package org.kexie.android.ftper.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.qmuiteam.qmui.util.QMUIKeyboardHelper;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.qmuiteam.qmui.widget.dialog.QMUITipDialog;

import org.kexie.android.ftper.BR;
import org.kexie.android.ftper.R;
import org.kexie.android.ftper.databinding.FragmentConfigBinding;
import org.kexie.android.ftper.databinding.ViewFooterConfigAddBinding;
import org.kexie.android.ftper.databinding.ViewHeadConfigBinding;
import org.kexie.android.ftper.viewmodel.ConfigViewModel;
import org.kexie.android.ftper.viewmodel.bean.ConfigItem;
import org.kexie.android.ftper.widget.ConfigDialogBuilder;
import org.kexie.android.ftper.widget.GenericQuickAdapter;
import org.kexie.android.ftper.widget.RxWrapper;
import org.kexie.android.ftper.widget.Utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import static android.view.View.OnClickListener;
import static com.chad.library.adapter.base.BaseQuickAdapter.OnItemClickListener;

public class ConfigFragment extends Fragment {

    private FragmentConfigBinding mBinding;

    private ViewFooterConfigAddBinding mFooterBinding;

    private ViewHeadConfigBinding mHeadBinding;

    private ConfigViewModel mViewModel;

    private GenericQuickAdapter<ConfigItem> mConfigAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConfigAdapter = new GenericQuickAdapter<>(R.layout.item_config, BR.configItem);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        //加载主视图
        mBinding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_config,
                container,
                false);
        //加载底部视图
        mFooterBinding = DataBindingUtil.inflate(inflater,
                R.layout.view_footer_config_add,
                mBinding.configs,
                false);
        //加载头部视图
        mHeadBinding = DataBindingUtil.inflate(inflater,
                R.layout.view_head_config,
                mBinding.configs,
                false);
        //设置他们
        mConfigAdapter.addFooterView(mFooterBinding.getRoot());
        mConfigAdapter.addHeaderView(mHeadBinding.getRoot());
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = ViewModelProviders.of(requireActivity())
                .get(ConfigViewModel.class);

        //订阅配置数据
        mViewModel.getConfigs().observe(this, mConfigAdapter::setNewData);

        mBinding.setAdapter(mConfigAdapter);

        mFooterBinding.setAddAction(RxWrapper
                .create(OnClickListener.class)
                .owner(this)
                .inner(v -> openConfigDialog(null))
                .build());

        mConfigAdapter.setOnItemClickListener(RxWrapper
                .create(OnItemClickListener.class)
                .owner(this)
                .inner((adapter, view12, position) ->
                {

                    ConfigItem configItem = (ConfigItem) adapter.getItem(position);
                    if (configItem != null) {
                        //找到上一次选择的项
                        int last = -1;
                        for (int i = 0; i < adapter.getData().size(); i++) {
                            ConfigItem item = (ConfigItem) adapter.getItem(i);
                            if (item != null && item.isSelect()) {
                                last = i;
                            }
                        }
                        //调用选择
                        mViewModel.select(configItem);
                        //然后更新这两个位置
                        if (last != -1) {
                            adapter.notifyItemChanged(
                                    adapter.getHeaderLayoutCount() + last);
                        }
                        adapter.notifyItemChanged(
                                adapter.getHeaderLayoutCount() + position);
                    }
                })
                .build());

        mConfigAdapter.setOnItemLongClickListener((adapter, view1, position) ->
        {
            ConfigItem configItem = (ConfigItem) adapter.getItem(position);
            if (configItem != null) {
                openConfigDialog(configItem);
                return true;
            }
            return false;
        });
    }

    //Resume时订阅,Pause时释放
    @Override
    public void onResume() {
        super.onResume();

        Utils.subscribeDialog(this,
                mViewModel.getOnError(),
                QMUITipDialog.Builder.ICON_TYPE_FAIL);

        Utils.subscribeDialog(this,
                mViewModel.getOnSuccess(),
                QMUITipDialog.Builder.ICON_TYPE_SUCCESS);

        Utils.subscribeDialog(this,
                mViewModel.getOnInfo(),
                QMUITipDialog.Builder.ICON_TYPE_INFO);
    }

    //打开配置对话框
    private void openConfigDialog(ConfigItem configItem) {
        boolean isAdd = configItem == null;
        // 防御性拷贝,防止内容更改影响原本的数据
        // 若是新建的则new一个
        ConfigItem backup = isAdd ? new ConfigItem() : configItem.clone();
        ConfigDialogBuilder builder = new ConfigDialogBuilder(requireContext());
        builder.addAction("保存", (dialog, index) ->
        {
            ConfigItem configItem2 = builder
                    .getBinding()
                    .getConfigItem();
            if (isAdd) {
                mViewModel.add(configItem2);
            } else {
                mViewModel.update(configItem2);
            }
            dialog.dismiss();
        });
        if (!isAdd) {
            builder.addAction("删除",
                    (dialog, index) -> new QMUIDialog.MessageDialogBuilder(requireContext())
                            .setTitle("提示")
                            .setMessage("确定要删除吗？")
                            .addAction("取消", (dialog1, index1) -> dialog1.dismiss())
                            .addAction(0,
                                    "删除", QMUIDialogAction.ACTION_PROP_NEGATIVE,
                                    (dialog12, index12) -> {
                                        dialog.dismiss();
                                        dialog12.dismiss();
                                        ConfigItem configItem2 = builder
                                                .getBinding()
                                                .getConfigItem();
                                        mViewModel.remove(configItem2);
                                    })
                            .create(com.qmuiteam.qmui.R.style.QMUI_Dialog)
                            .show()).setTitle("修改服务器信息");
        } else {
            builder.setTitle("添加服务器");
        }
        builder.addAction("取消", (dialog, index) -> dialog.dismiss())
                .create(com.qmuiteam.qmui.R.style.QMUI_Dialog)
                .show();
        QMUIKeyboardHelper.showKeyboard(builder.getBinding().host, true);
        builder.getBinding().setConfigItem(backup);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mConfigAdapter.removeHeaderView(mHeadBinding.getRoot());
        mConfigAdapter.removeFooterView(mFooterBinding.getRoot());
        mHeadBinding.unbind();
        mHeadBinding = null;
        mFooterBinding.unbind();
        mFooterBinding = null;
        mBinding.unbind();
        mBinding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mConfigAdapter = null;
    }
}
