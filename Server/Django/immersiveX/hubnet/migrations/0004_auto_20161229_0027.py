# -*- coding: utf-8 -*-
# Generated by Django 1.10.4 on 2016-12-28 23:27
from __future__ import unicode_literals

from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [
        ('hubnet', '0003_auto_20161229_0026'),
    ]

    operations = [
        migrations.RenameField(
            model_name='event',
            old_name='interestTags',
            new_name='group',
        ),
    ]
