# Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
# All rights reserved.
# PROPRIETARY AND CONFIDENTIAL.
# No part of this software may be reproduced, distributed, or transmitted
# in any form or by any means without express written permission.

"""RDS stop/start — dev 10h/dia (ADR-0014).

Acionado por EventBridge (cron). O evento informa `action` = "start" | "stop".
A instância alvo vem da variável de ambiente DB_INSTANCE_IDENTIFIER.
"""

import os

import boto3

INSTANCE_ID = os.environ["DB_INSTANCE_IDENTIFIER"]
rds = boto3.client("rds")


def handler(event, context):
    action = event.get("action")

    try:
        if action == "start":
            rds.start_db_instance(DBInstanceIdentifier=INSTANCE_ID)
        elif action == "stop":
            rds.stop_db_instance(DBInstanceIdentifier=INSTANCE_ID)
        else:
            raise ValueError(f"action inválida: {action!r}")
    except rds.exceptions.InvalidDBInstanceStateFault:
        # já no estado desejado (ou em transição) — no-op idempotente
        return {"action": action, "instance": INSTANCE_ID, "status": "noop"}

    return {"action": action, "instance": INSTANCE_ID, "status": "ok"}
