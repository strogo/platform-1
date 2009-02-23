package test;

import platform.server.view.navigator.NavigatorForm;
import platform.server.view.form.ObjectImplement;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.DefaultClientFormView;
import platform.server.logics.classes.DataClass;

class IntegralNavigatorForm extends NavigatorForm<TestBusinessLogics> {

    IntegralNavigatorForm(int iID, String caption, TestBusinessLogics BL) {
        super(iID, caption);

        ObjectImplement obj1 = new ObjectImplement(IDShift(1), DataClass.date);
        obj1.caption = "дата 1";

        ObjectImplement obj2 = new ObjectImplement(IDShift(1), DataClass.date);
        obj2.caption = "дата 2";

        ObjectImplement obj3 = new ObjectImplement(IDShift(1),BL.Article);
        obj3.caption = "товар";

        GroupObjectImplement gv = new GroupObjectImplement(IDShift(1));
        GroupObjectImplement gv2 = new GroupObjectImplement(IDShift(1));

        gv.addObject(obj1);
        gv.addObject(obj2);
        addGroup(gv);
        gv.gridClassView = false;

        gv2.addObject(obj3);
        addGroup(gv2);

        BL.fillSingleViews(obj3,this,null);

        BL.addPropertyView(this,BL.RashArtInt,gv2,obj3,obj1,obj2);

        DefaultClientFormView formView = new DefaultClientFormView(this);
        formView.get(gv).singleViewType = true;

        richDesign = formView;

    }

}
