# -*- coding: utf-8 -*-
# Generated by Django 1.10.4 on 2017-01-09 10:07
from __future__ import unicode_literals

from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [
        ('hubnet', '0013_auto_20170109_1101'),
    ]

    operations = [
        migrations.RenameField(
            model_name='sensor',
            old_name='description',
            new_name='name',
        ),
    ]