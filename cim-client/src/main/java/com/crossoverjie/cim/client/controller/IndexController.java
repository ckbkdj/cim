package com.crossoverjie.cim.client.controller;

import com.crossoverjie.cim.client.client.CIMClient;
import com.crossoverjie.cim.client.service.RouteRequest;
import com.crossoverjie.cim.client.vo.req.GoogleProtocolVO;
import com.crossoverjie.cim.client.vo.req.StringReqVO;
import com.crossoverjie.cim.client.vo.res.SendMsgResVO;
import com.crossoverjie.cim.common.enums.StatusEnum;
import com.crossoverjie.cim.common.res.BaseResponse;
import com.crossoverjie.cim.common.res.NULLBody;
import com.crossoverjie.cim.route.api.vo.req.ChatReqVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Function:
 *
 * @author crossoverJie
 *         Date: 22/05/2018 14:46
 * @since JDK 1.8
 */
@Controller
@RequestMapping("/")
@ConditionalOnWebApplication
public class IndexController {


    @Autowired
    private CIMClient heartbeatClient ;



    @Autowired
    private RouteRequest routeRequest ;


    /**
     * 向服务端发消息 字符串
     * @param stringReqVO
     * @return
     */
    @Operation(summary = "客户端发送消息，字符串")
    @RequestMapping(value = "sendStringMsg", method = RequestMethod.POST)
    @ResponseBody
    public BaseResponse<NULLBody> sendStringMsg(@RequestBody StringReqVO stringReqVO){
        BaseResponse<NULLBody> res = new BaseResponse();

        for (int i = 0; i < 100; i++) {
            heartbeatClient.sendStringMsg(stringReqVO.getMsg()) ;
        }

        // TODO: 2024/5/30 metrics

        SendMsgResVO sendMsgResVO = new SendMsgResVO() ;
        sendMsgResVO.setMsg("OK") ;
        res.setCode(StatusEnum.SUCCESS.getCode()) ;
        res.setMessage(StatusEnum.SUCCESS.getMessage()) ;
        return res ;
    }

    /**
     * 向服务端发消息 Google ProtoBuf
     * @param googleProtocolVO
     * @return
     */
    @Operation(summary = "向服务端发消息 Google ProtoBuf")
    @RequestMapping(value = "sendProtoBufMsg", method = RequestMethod.POST)
    @ResponseBody
    public BaseResponse<NULLBody> sendProtoBufMsg(@RequestBody GoogleProtocolVO googleProtocolVO){
        BaseResponse<NULLBody> res = new BaseResponse();

        for (int i = 0; i < 100; i++) {
            heartbeatClient.sendGoogleProtocolMsg(googleProtocolVO) ;
        }

        // TODO: 2024/5/30 metrics

        SendMsgResVO sendMsgResVO = new SendMsgResVO() ;
        sendMsgResVO.setMsg("OK") ;
        res.setCode(StatusEnum.SUCCESS.getCode()) ;
        res.setMessage(StatusEnum.SUCCESS.getMessage()) ;
        return res ;
    }



    /**
     * 群发消息
     * @param chatReqVO
     * @return
     */
    @Operation(summary = "群发消息")
    @RequestMapping(value = "sendGroupMsg",method = RequestMethod.POST)
    @ResponseBody
    public BaseResponse sendGroupMsg(@RequestBody ChatReqVO chatReqVO) throws Exception {
        BaseResponse<NULLBody> res = new BaseResponse();
        routeRequest.sendGroupMsg(chatReqVO) ;

        // TODO: 2024/5/30 metrics

        res.setCode(StatusEnum.SUCCESS.getCode()) ;
        res.setMessage(StatusEnum.SUCCESS.getMessage()) ;
        return res ;
    }
}
