package com.cheng.o2o.web.shopadmin;

import com.cheng.o2o.dto.ImageHolder;
import com.cheng.o2o.dto.ShopExecution;
import com.cheng.o2o.entity.Area;
import com.cheng.o2o.entity.PersonInfo;
import com.cheng.o2o.entity.Shop;
import com.cheng.o2o.entity.ShopCategory;
import com.cheng.o2o.enums.ShopStateEnum;
import com.cheng.o2o.exceptions.ShopOperationException;
import com.cheng.o2o.service.AreaService;
import com.cheng.o2o.service.ShopCategoryService;
import com.cheng.o2o.service.ShopService;
import com.cheng.o2o.util.CodeUtil;
import com.cheng.o2o.util.HttpServletRequestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author cheng
 *         2018/3/29 18:06
 */
@Controller
@RequestMapping("/shop")
public class ShopManagementController {

    @Autowired
    private ShopService shopService;
    @Autowired
    private ShopCategoryService shopCategoryService;
    @Autowired
    private AreaService areaService;

    @GetMapping("/getshopinitinfo")
    @ResponseBody
    private Map<String, Object> getShopInitInfo() {

        Map<String, Object> modelMap = new HashMap<>(4);
        List<ShopCategory> shopCategoryList;
        List<Area> areaList;

        try {
            shopCategoryList = shopCategoryService.getShopCategoryList(new ShopCategory());
            areaList = areaService.getAreaList();
            modelMap.put("shopCategoryList", shopCategoryList);
            modelMap.put("areaList", areaList);
            modelMap.put("success", true);
        } catch (Exception e) {
            modelMap.put("success", false);
            modelMap.put("errMsg", e.getMessage());
        }

        return modelMap;
    }

    @PostMapping("/registershop")
    @ResponseBody
    private Map<String, Object> registerShop(HttpServletRequest request) {

        Map<String, Object> modelMap = new HashMap<>(4);

        // 校验验证码
        if (!CodeUtil.checkVerifyCode(request)) {
            modelMap.put("success", false);
            modelMap.put("errMsg", "验证码有误!");
            return modelMap;
        }

        // 1.接收并转换相应的参数,包括店铺信息以及图片信息
        String shopStr = HttpServletRequestUtil.getString(request, "shopStr");
        ObjectMapper mapper = new ObjectMapper();
        Shop shop;
        try {
            shop = mapper.readValue(shopStr, Shop.class);
        } catch (Exception e) {
            modelMap.put("success", false);
            // TODO 错误信息返回修改
            modelMap.put("errMsg", e.getMessage());
            return modelMap;
        }

        CommonsMultipartFile shopImg;
        CommonsMultipartResolver commonsMultipartResolver =
                new CommonsMultipartResolver(request.getSession().getServletContext());
        if (commonsMultipartResolver.isMultipart(request)) {
            MultipartHttpServletRequest multipartHttpServletRequest =
                    (MultipartHttpServletRequest) request;
            shopImg = (CommonsMultipartFile) multipartHttpServletRequest.getFile("shopImg");
        } else {
            modelMap.put("success", false);
            modelMap.put("errMsg", "上传图片不能为空");
            return modelMap;
        }

        // 2.注册店铺
        if (shop != null && shopImg != null) {
            PersonInfo owner = (PersonInfo) request.getSession().getAttribute("user");
            shop.setOwner(owner);

            try {
                ShopExecution se = shopService.addShop(shop, new ImageHolder(shopImg.getOriginalFilename(), shopImg.getInputStream()));
                if (se.getState() == ShopStateEnum.CHECK.getState()) {
                    modelMap.put("success", true);
                    // 该用户可以操作的店铺列表
                    @SuppressWarnings("unchecked")
                    List<Shop> shopList = (List<Shop>) request.getSession().getAttribute("shopList");
                    if (shopList == null || shopList.size() == 0) {
                        shopList = new ArrayList<>();
                    }
                    shopList.add(se.getShop());
                    request.getSession().setAttribute("shopList", shopList);

                } else {
                    modelMap.put("success", false);
                    modelMap.put("errMsg", se.getStateInfo());
                }
            } catch (ShopOperationException | IOException e) {
                modelMap.put("success", false);
                modelMap.put("errMsg", e.getMessage());
            }
        } else {
            modelMap.put("success", false);
            modelMap.put("errMsg", "请输入店铺信息");
        }

        return modelMap;
    }


    @PostMapping("/modifyshop")
    @ResponseBody
    private Map<String, Object> modifyShop(HttpServletRequest request) {

        Map<String, Object> modelMap = new HashMap<>(4);

        // 校验验证码
        if (!CodeUtil.checkVerifyCode(request)) {
            modelMap.put("success", false);
            modelMap.put("errMsg", "验证码有误!");
            return modelMap;
        }

        // 1.接收并转换相应的参数,包括店铺信息以及图片信息
        String shopStr = HttpServletRequestUtil.getString(request, "shopStr");
        ObjectMapper mapper = new ObjectMapper();
        Shop shop;
        try {
            shop = mapper.readValue(shopStr, Shop.class);
        } catch (Exception e) {
            modelMap.put("success", false);
            // TODO 错误信息返回修改
            modelMap.put("errMsg", e.getMessage());
            return modelMap;
        }

        CommonsMultipartFile shopImg = null;
        CommonsMultipartResolver commonsMultipartResolver =
                new CommonsMultipartResolver(request.getSession().getServletContext());
        if (commonsMultipartResolver.isMultipart(request)) {
            MultipartHttpServletRequest multipartHttpServletRequest =
                    (MultipartHttpServletRequest) request;
            shopImg = (CommonsMultipartFile) multipartHttpServletRequest.getFile("shopImg");
        }

        // 2.修改店铺信息
        if (shop != null && shop.getShopId() != null) {
            ShopExecution se;
            try {
                if (shopImg == null) {
                    se = shopService.modifyShop(shop, new ImageHolder(null, null));
                } else {
                    se = shopService.modifyShop(shop, new ImageHolder(shopImg.getOriginalFilename(),shopImg.getInputStream()));
                }

                if (se.getState() == ShopStateEnum.SUCCESS.getState()) {
                    modelMap.put("success", true);
                } else {
                    modelMap.put("success", false);
                    modelMap.put("errMsg", se.getStateInfo());
                }
            } catch (ShopOperationException | IOException e) {
                modelMap.put("success", false);
                modelMap.put("errMsg", e.getMessage());
                System.out.println(e.getMessage());
            }
        } else {
            modelMap.put("success", false);
            modelMap.put("errMsg", "请输入店铺信息");
        }

        return modelMap;
    }

    @GetMapping("/getshopmanagementinfo")
    @ResponseBody
    private Map<String, Object> getShopManagementInfo(HttpServletRequest request) {
        Map<String, Object> modelMap = new HashMap<>(4);
        long shopId = HttpServletRequestUtil.getLong(request, "shopId");
        if (shopId <= 0) {
            Object currentShopObj = request.getSession().getAttribute("currentShop");
            if (currentShopObj == null) {
                modelMap.put("redirect", true);
                modelMap.put("url", "/o2o/shopadmin/shoplist");
            } else {
                Shop currentShop = (Shop) currentShopObj;
                modelMap.put("redirect", false);
                modelMap.put("shopId", currentShop.getShopId());
            }
        } else {
            Shop currentShop = new Shop();
            currentShop.setShopId(shopId);
            request.getSession().setAttribute("currentShop", currentShop);
            modelMap.put("redirect", false);
        }

        return modelMap;
    }

    @GetMapping("/getshopbyid")
    @ResponseBody
    private Map<String, Object> getShopById(HttpServletRequest request) {
        Map<String, Object> modelMap = new HashMap<>(4);
        Long shopId = HttpServletRequestUtil.getLong(request, "shopId");
        if (shopId > -1) {
            try {
                Shop shop = shopService.getByShopId(shopId);
                List<Area> areaList = areaService.getAreaList();
                modelMap.put("shop", shop);
                modelMap.put("areaList", areaList);
                modelMap.put("success", true);
            } catch (Exception e) {
                modelMap.put("success", false);
                modelMap.put("errMsg", e.toString());
            }
        } else {
            modelMap.put("success", false);
            modelMap.put("errMsg", "empty shopId");
        }

        return modelMap;
    }

    @GetMapping("/getshoplist")
    @ResponseBody
    private Map<String, Object> getShopList(HttpServletRequest request) {
        Map<String, Object> modelMap = new HashMap<>(4);
        PersonInfo user = (PersonInfo) request.getSession().getAttribute("user");
        long employedId = user.getUserId();

        try {
            Shop shopCondition = new Shop();
            shopCondition.setOwner(user);
            ShopExecution se = shopService.getShopList(shopCondition, 0, 100);
            // 列出店铺成功后，将店铺存放如session中作为权限验证的依据，即该帐号只操作它自己的店铺
            request.getSession().setAttribute("shopList", se.getShopList());

            modelMap.put("success", true);
            modelMap.put("shopList", se.getShopList());
            modelMap.put("user", user);
        } catch (Exception e) {
            modelMap.put("success", false);
            modelMap.put("errMsg", e.getMessage());
        }

        return modelMap;
    }
}