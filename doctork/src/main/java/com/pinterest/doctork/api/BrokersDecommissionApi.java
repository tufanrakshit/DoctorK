package com.pinterest.doctork.api;

import com.pinterest.doctork.DoctorK;
import com.pinterest.doctork.config.DoctorKConfig;
import com.pinterest.doctork.util.ApiUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/clusters/{clusterName}/brokers/{brokerId}/admin/decommission")
@Produces({MediaType.APPLICATION_JSON })
@Consumes({MediaType.APPLICATION_JSON })
public class BrokersDecommissionApi extends DoctorKApi {

  private static final Logger LOG = LogManager.getLogger(BrokersDecommissionApi.class);

  public BrokersDecommissionApi(DoctorK doctorK) {
    super(doctorK);
  }

  @GET
  public boolean isBrokerDecommissioned(@PathParam("clusterName") String clusterName, @PathParam("brokerId") String brokerId) {
    return checkAndGetBroker(clusterName, brokerId).isDecommissioned();
  }

  @PUT
  @RolesAllowed({ DoctorKConfig.DOCTORK_ADMIN_ROLE})
  public void decommissionBroker(@Context HttpServletRequest ctx,
                                 @PathParam("clusterName") String clusterName,
                                 @PathParam("brokerId") String brokerIdStr) {
    checkAndGetClusterManager(clusterName).decommissionBroker(Integer.parseInt(brokerIdStr));
    ApiUtils.logAPIAction(LOG, ctx, "Decommissioned for broker:" + brokerIdStr + " on cluster "+ clusterName);
  }

  @DELETE
  @RolesAllowed({ DoctorKConfig.DOCTORK_ADMIN_ROLE})
  public void cancelDecommissionBroker(@Context HttpServletRequest ctx,
                                       @PathParam("clusterName") String clusterName,
                                       @PathParam("brokerId") String brokerIdStr) {
    checkAndGetClusterManager(clusterName).cancelDecommissionBroker(Integer.parseInt(brokerIdStr));
    ApiUtils.logAPIAction(LOG, ctx, "Decommissioned cancelled for broker:" + brokerIdStr + " on cluster "+ clusterName);
  }

}
